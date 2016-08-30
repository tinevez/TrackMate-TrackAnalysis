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
import net.imglib2.multithreading.SimpleMultiThreading;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class, priority = 1d )
public class BasicTrackAnalysis implements TrackAnalyzer
{

	public static final String KEY = "Track analysis";

	public static final String TRACK_TOTAL_DISTANCE_TRAVELED = "TOTAL_DISTANCE_TRAVELED";

	public static final String TRACK_MAX_DISTANCE_TRAVELED = "MAX_DISTANCE_TRAVELED";

	public static final String TRACK_CONFINMENT_RATIO = "CONFINMENT_RATIO";

	public static final String TRACK_MEAN_STRAIGHT_LINE_SPEED = "MEAN_STRAIGHT_LINE_SPEED";

	public static final String TRACK_LINEARITY_OF_FORWARD_PROGRESSION = "LINEARITY_OF_FORWARD_PROGRESSION";

	public static final List< String > FEATURES = new ArrayList< String >( 5 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 5 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 5 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 5 );

	public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 4 );

	static
	{
		FEATURES.add( TRACK_TOTAL_DISTANCE_TRAVELED );
		FEATURES.add( TRACK_MAX_DISTANCE_TRAVELED );
		FEATURES.add( TRACK_CONFINMENT_RATIO );
		FEATURES.add( TRACK_MEAN_STRAIGHT_LINE_SPEED );
		FEATURES.add( TRACK_LINEARITY_OF_FORWARD_PROGRESSION );

		FEATURE_NAMES.put( TRACK_TOTAL_DISTANCE_TRAVELED, "Total distance traveled" );
		FEATURE_NAMES.put( TRACK_MAX_DISTANCE_TRAVELED, "Max distance traveled" );
		FEATURE_NAMES.put( TRACK_CONFINMENT_RATIO, "Confinment ratio" );
		FEATURE_NAMES.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, "Mean straight line speed" );
		FEATURE_NAMES.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, "Linearity of forward progression" );

		FEATURE_SHORT_NAMES.put( TRACK_TOTAL_DISTANCE_TRAVELED, "Total dist." );
		FEATURE_SHORT_NAMES.put( TRACK_MAX_DISTANCE_TRAVELED, "Max dist." );
		FEATURE_SHORT_NAMES.put( TRACK_CONFINMENT_RATIO, "Cnfnmnt ratio" );
		FEATURE_SHORT_NAMES.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, "Mean v. line" );
		FEATURE_SHORT_NAMES.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, "Lin. fwd. progr." );

		FEATURE_DIMENSIONS.put( TRACK_TOTAL_DISTANCE_TRAVELED, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( TRACK_MAX_DISTANCE_TRAVELED, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( TRACK_CONFINMENT_RATIO, Dimension.NONE );
		FEATURE_DIMENSIONS.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, Dimension.NONE );

		IS_INT.put( TRACK_TOTAL_DISTANCE_TRAVELED, Boolean.FALSE );
		IS_INT.put( TRACK_MAX_DISTANCE_TRAVELED, Boolean.FALSE );
		IS_INT.put( TRACK_CONFINMENT_RATIO, Boolean.FALSE );
		IS_INT.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, Boolean.FALSE );
		IS_INT.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	public BasicTrackAnalysis()
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
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{
						/*
						 * Get the first spot (lower FRAME).
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

						// Store.
						fm.putTrackFeature( trackID, TRACK_TOTAL_DISTANCE_TRAVELED, totalDistance );
						fm.putTrackFeature( trackID, TRACK_MAX_DISTANCE_TRAVELED, maxDistance );
						fm.putTrackFeature( trackID, TRACK_CONFINMENT_RATIO, confinmentRatio );
						fm.putTrackFeature( trackID, TRACK_MEAN_STRAIGHT_LINE_SPEED, meanStraightLineSpeed );
						fm.putTrackFeature( trackID, TRACK_LINEARITY_OF_FORWARD_PROGRESSION, linearityForwardProgression );

					}
				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin( threads );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}
}
