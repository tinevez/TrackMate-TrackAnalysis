package fiji.plugin.trackmate.features.edge;

import java.util.ArrayList;
import java.util.Collection;
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
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import net.imglib2.multithreading.SimpleMultiThreading;

@SuppressWarnings( "deprecation" )
@Plugin( type = EdgeAnalyzer.class )
public class LinearTrackEdgeStatistics implements EdgeAnalyzer
{

	public static final String KEY = "Linear track edge analysis";

	public static final String DIRECTIONAL_CHANGE_RATE = "DIRECTIONAL_CHANGE_RATE";

	public static final String ABSOLUTE_ANGLE_XY = "ABSOLUTE_ANGLE_XY";

	public static final String ABSOLUTE_ANGLE_YZ = "ABSOLUTE_ANGLE_YZ";

	public static final String ABSOLUTE_ANGLE_ZX = "ABSOLUTE_ANGLE_ZX";

	public static final List< String > FEATURES = new ArrayList<>( 4 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 4 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 4 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 4 );

	public static final Map< String, Boolean > IS_INT = new HashMap<>( 4 );

	static
	{
		FEATURES.add( DIRECTIONAL_CHANGE_RATE );
		FEATURES.add( ABSOLUTE_ANGLE_XY );
		FEATURES.add( ABSOLUTE_ANGLE_YZ );
		FEATURES.add( ABSOLUTE_ANGLE_ZX );

		FEATURE_NAMES.put( DIRECTIONAL_CHANGE_RATE, "Directional change rate" );
		FEATURE_NAMES.put( ABSOLUTE_ANGLE_XY, "Absolute angle in xy plane" );
		FEATURE_NAMES.put( ABSOLUTE_ANGLE_YZ, "Absolute angle in yz plane" );
		FEATURE_NAMES.put( ABSOLUTE_ANGLE_ZX, "Absolute angle in zx plane" );

		FEATURE_SHORT_NAMES.put( DIRECTIONAL_CHANGE_RATE, "𝛾 rate" );
		FEATURE_SHORT_NAMES.put( ABSOLUTE_ANGLE_XY, "Angle xy" );
		FEATURE_SHORT_NAMES.put( ABSOLUTE_ANGLE_YZ, "Angle yz" );
		FEATURE_SHORT_NAMES.put( ABSOLUTE_ANGLE_ZX, "Angle zx" );

		FEATURE_DIMENSIONS.put( DIRECTIONAL_CHANGE_RATE, Dimension.RATE );
		FEATURE_DIMENSIONS.put( ABSOLUTE_ANGLE_XY, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( ABSOLUTE_ANGLE_YZ, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( ABSOLUTE_ANGLE_ZX, Dimension.ANGLE );

		IS_INT.put( DIRECTIONAL_CHANGE_RATE, Boolean.FALSE );
		IS_INT.put( ABSOLUTE_ANGLE_XY, Boolean.FALSE );
		IS_INT.put( ABSOLUTE_ANGLE_YZ, Boolean.FALSE );
		IS_INT.put( ABSOLUTE_ANGLE_ZX, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	public LinearTrackEdgeStatistics()
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
	public void process( final Collection< DefaultWeightedEdge > edges, final Model model )
	{
		if ( edges.isEmpty() )
		{ return; }

		final FeatureModel featureModel = model.getFeatureModel();
		// Neighbor index, for predecessor retrieval.
		final TimeDirectedNeighborIndex neighborIndex = model.getTrackModel().getDirectedNeighborIndex();

		final ArrayBlockingQueue< DefaultWeightedEdge > queue = new ArrayBlockingQueue<>( edges.size(), false, edges );

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( KEY + " thread " + i )
			{
				@Override
				public void run()
				{
					// Storage array for 3D angle calculation.
					final double[] out = new double[ 3 ];

					DefaultWeightedEdge edge;
					while ( ( edge = queue.poll() ) != null )
					{
						Spot source = model.getTrackModel().getEdgeSource( edge );
						Spot target = model.getTrackModel().getEdgeTarget( edge );

						// Some edges maybe improperly oriented.
						if ( source.diffTo( target, Spot.FRAME ) > 0 )
						{
							final Spot tmp = target;
							target = source;
							source = tmp;
						}

						/*
						 * Edge absolute angle.
						 */

						final double dx2 = target.diffTo( source, Spot.POSITION_X );
						final double dy2 = target.diffTo( source, Spot.POSITION_Y );
						final double dz2 = target.diffTo( source, Spot.POSITION_Z );

						final double angleXY = Math.atan2( dy2, dx2 );
						final double angleYZ = Math.atan2( dz2, dy2 );
						final double angleZX = Math.atan2( dx2, dz2 );

						featureModel.putEdgeFeature( edge, ABSOLUTE_ANGLE_XY, Double.valueOf( angleXY ) );
						featureModel.putEdgeFeature( edge, ABSOLUTE_ANGLE_YZ, Double.valueOf( angleYZ ) );
						featureModel.putEdgeFeature( edge, ABSOLUTE_ANGLE_ZX, Double.valueOf( angleZX ) );

						/*
						 * Rate of directional change. We need to fetch the
						 * previous edge, via the source.
						 */

						final Set< Spot > predecessors = neighborIndex.predecessorsOf( source );
						if ( null == predecessors || predecessors.size() != 1 )
						{
							featureModel.putEdgeFeature( edge, DIRECTIONAL_CHANGE_RATE, Double.NaN );
							continue;
						}

						/*
						 * We take the first predecessor. The directional change
						 * is anyway not defined in case of branching.
						 */
						final Spot predecessor = predecessors.iterator().next();

						// Vectors.
						final double dx1 = source.diffTo( predecessor, Spot.POSITION_X );
						final double dy1 = source.diffTo( predecessor, Spot.POSITION_Y );
						final double dz1 = source.diffTo( predecessor, Spot.POSITION_Z );


						crossProduct( dx1, dy1, dz1, dx2, dy2, dz2, out );
						final double deltaAlpha = Math.atan2( norm( out ), dotProduct( dx1, dy1, dz1, dx2, dy2, dz2 ) );
						final double angleSpeed = deltaAlpha / target.diffTo( source, Spot.POSITION_T );

						featureModel.putEdgeFeature( edge, DIRECTIONAL_CHANGE_RATE, Double.valueOf( angleSpeed ) );
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
