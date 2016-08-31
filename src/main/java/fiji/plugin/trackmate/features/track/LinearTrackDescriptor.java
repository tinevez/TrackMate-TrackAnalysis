package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import net.imglib2.multithreading.SimpleMultiThreading;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class, priority = 1d )
public class LinearTrackDescriptor implements TrackAnalyzer
{

	public static final String KEY = "Linear track analysis";

	public static final String TRACK_TOTAL_DISTANCE_TRAVELED = "TOTAL_DISTANCE_TRAVELED";

	public static final String TRACK_MAX_DISTANCE_TRAVELED = "MAX_DISTANCE_TRAVELED";

	public static final String TRACK_CONFINMENT_RATIO = "CONFINMENT_RATIO";

	public static final String TRACK_MEAN_STRAIGHT_LINE_SPEED = "MEAN_STRAIGHT_LINE_SPEED";

	public static final String TRACK_LINEARITY_OF_FORWARD_PROGRESSION = "LINEARITY_OF_FORWARD_PROGRESSION";

	public static final String TRACK_MEAN_DIRECTIONAL_CHANGE_RATE = "MEAN_DIRECTIONAL_CHANGE_RATE";

	public static final List< String > FEATURES = new ArrayList< String >( 6 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 6 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 6 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 6 );

	public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 6 );


	static
	{
		FEATURES.add( TRACK_TOTAL_DISTANCE_TRAVELED );
		FEATURES.add( TRACK_MAX_DISTANCE_TRAVELED );
		FEATURES.add( TRACK_CONFINMENT_RATIO );
		FEATURES.add( TRACK_MEAN_STRAIGHT_LINE_SPEED );
		FEATURES.add( TRACK_LINEARITY_OF_FORWARD_PROGRESSION );
		FEATURES.add( TRACK_MEAN_DIRECTIONAL_CHANGE_RATE );

		FEATURE_NAMES.put( TRACK_TOTAL_DISTANCE_TRAVELED, "Total distance traveled" );
		FEATURE_NAMES.put( TRACK_MAX_DISTANCE_TRAVELED, "Max distance traveled" );
		FEATURE_NAMES.put( TRACK_CONFINMENT_RATIO, "Confinment ratio" );
		FEATURE_NAMES.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, "Mean straight line speed" );
		FEATURE_NAMES.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, "Linearity of forward progression" );
		FEATURE_NAMES.put( TRACK_MEAN_DIRECTIONAL_CHANGE_RATE, "Mean directional change rate" );

		FEATURE_SHORT_NAMES.put( TRACK_TOTAL_DISTANCE_TRAVELED, "Total dist." );
		FEATURE_SHORT_NAMES.put( TRACK_MAX_DISTANCE_TRAVELED, "Max dist." );
		FEATURE_SHORT_NAMES.put( TRACK_CONFINMENT_RATIO, "Cnfnmnt ratio" );
		FEATURE_SHORT_NAMES.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, "Mean v. line" );
		FEATURE_SHORT_NAMES.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, "Lin. fwd. progr." );
		FEATURE_SHORT_NAMES.put( TRACK_MEAN_DIRECTIONAL_CHANGE_RATE, "Mean 𝛾 rate" );

		FEATURE_DIMENSIONS.put( TRACK_TOTAL_DISTANCE_TRAVELED, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( TRACK_MAX_DISTANCE_TRAVELED, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( TRACK_CONFINMENT_RATIO, Dimension.NONE );
		FEATURE_DIMENSIONS.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, Dimension.NONE );
		FEATURE_DIMENSIONS.put( TRACK_MEAN_DIRECTIONAL_CHANGE_RATE, Dimension.RATE );

		IS_INT.put( TRACK_TOTAL_DISTANCE_TRAVELED, Boolean.FALSE );
		IS_INT.put( TRACK_MAX_DISTANCE_TRAVELED, Boolean.FALSE );
		IS_INT.put( TRACK_CONFINMENT_RATIO, Boolean.FALSE );
		IS_INT.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, Boolean.FALSE );
		IS_INT.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, Boolean.FALSE );
		IS_INT.put( TRACK_MEAN_DIRECTIONAL_CHANGE_RATE, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	public LinearTrackDescriptor()
	{
		setNumThreads();
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getInfoText()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;

	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{

		if ( trackIDs.isEmpty() ) { return; }

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< Integer >( trackIDs.size(), false, trackIDs );
		final FeatureModel fm = model.getFeatureModel();

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( KEY + " thread " + i )
			{
				@Override
				public void run()
				{
					// Neighbor index, for predecessor retrieval.
					final TimeDirectedNeighborIndex neighborIndex = model.getTrackModel().getDirectedNeighborIndex();
					// Storage array for 3D angle calculation.
					final double[] out = new double[ 3 ];

					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{
						/*
						 * Get the first spot (lowest FRAME).
						 */

						final List< Spot > spots = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
						Collections.sort( spots, Spot.frameComparator );
						final Spot first = spots.get( 0 );
						
						/*
						 * Iterate over edges.
						 */

						final Set< DefaultWeightedEdge > edges = model.getTrackModel().trackEdges( trackID );

						double totalDistance = 0.;
						double maxDistanceSq = Double.NEGATIVE_INFINITY;
						double maxDistance = 0.;
						double sumAngleSpeed = 0.;
						int nAngleSpeed = 0;

						for ( final DefaultWeightedEdge edge : edges )
						{
							// Total distance travelled.
							final Spot source = model.getTrackModel().getEdgeSource( edge );
							final Spot target = model.getTrackModel().getEdgeTarget( edge );
							final double d = Math.sqrt( source.squareDistanceTo( target ) );
							totalDistance += d;

							// Max distance traveled.
							final double dToFirstSq = first.squareDistanceTo( target );
							if ( dToFirstSq > maxDistanceSq )
							{
								maxDistanceSq = dToFirstSq;
								maxDistance = Math.sqrt( maxDistanceSq );
							}

							/*
							 * Rate of directional change. We need to fetch the
							 * previous edge, via the source.
							 */

							final Set< Spot > predecessors = neighborIndex.predecessorsOf( source );
							if (null != predecessors && !predecessors.isEmpty())
							{
								/*
								 * We take the first predecessor. The
								 * directional change is anyway not defined in
								 * case of branching.
								 */

								final Spot predecessor = predecessors.iterator().next();
								
								// Vectors.
								final double dx1 = first.diffTo( predecessor, Spot.POSITION_X );
								final double dy1 = first.diffTo( predecessor, Spot.POSITION_Y );
								final double dz1 = first.diffTo( predecessor, Spot.POSITION_Z );

								final double dx2 = target.diffTo( first, Spot.POSITION_X );
								final double dy2 = target.diffTo( first, Spot.POSITION_Y );
								final double dz2 = target.diffTo( first, Spot.POSITION_Z );

								crossProduct( dx1, dy1, dz1, dx2, dy2, dz2, out );
								final double deltaAlpha = Math.atan2( norm( out ), dotProduct( dx1, dy1, dz1, dx2, dy2, dz2 ) );
								final double angleSpeed = deltaAlpha / target.diffTo( first, Spot.POSITION_T );
								sumAngleSpeed += angleSpeed;
								nAngleSpeed++;
							}

						}

						/*
						 * Compute features.
						 */

						// Dependency features.
						final double netDistance = fm.getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_DISPLACEMENT );
						final double tTotal = fm.getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_DURATION );
						final double vMean = fm.getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED );

						// Our features.
						final double confinmentRatio = netDistance / totalDistance;
						final double meanStraightLineSpeed = netDistance / tTotal;
						final double linearityForwardProgression = meanStraightLineSpeed / vMean;
						final double meanAngleSpeed = sumAngleSpeed / nAngleSpeed;

						// Store.
						fm.putTrackFeature( trackID, TRACK_TOTAL_DISTANCE_TRAVELED, totalDistance );
						fm.putTrackFeature( trackID, TRACK_MAX_DISTANCE_TRAVELED, maxDistance );
						fm.putTrackFeature( trackID, TRACK_CONFINMENT_RATIO, confinmentRatio );
						fm.putTrackFeature( trackID, TRACK_MEAN_STRAIGHT_LINE_SPEED, meanStraightLineSpeed );
						fm.putTrackFeature( trackID, TRACK_LINEARITY_OF_FORWARD_PROGRESSION, linearityForwardProgression );
						fm.putTrackFeature( trackID, TRACK_MEAN_DIRECTIONAL_CHANGE_RATE, meanAngleSpeed );

					}
				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin( threads );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	private static final double dotProduct( final double dx1, final double dy1, final double dz1, final double dx2, final double dy2, final double dz2 )
	{
		return dx1 * dx2 + dy1 * dy2 + dz1 * dz2;
	}

	private static final void crossProduct( final double dx1, final double dy1, final double dz1, final double dx2, final double dy2, final double dz2, final double[] out )
	{
		out[ 0 ] = dy1 * dz2 - dz1 * dy2;
		out[ 1 ] = dz1 * dx2 - dx1 * dz2;
		out[ 2 ] = dx1 * dy2 - dy1 * dx2;
	}

	private static final double norm( final double[] v )
	{
		double sumSq = 0.;
		for ( final double d : v )
		{
			sumSq += d * d;
		}
		return Math.sqrt( sumSq );
	}
}
