package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import net.imglib2.multithreading.SimpleMultiThreading;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class, priority = 1d )
public class BasicTrackAnalysis implements TrackAnalyzer
{

	public static final String KEY = "Track analysis";

	public static final String TRACK_TOTAL_DISTANCE_TRAVELLED = "TOTAL_DISTANCE_TRAVELLED";

	public static final String TRACK_MAX_DISTANCE_TRAVELLED = "MAX_DISTANCE_TRAVELLED";

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
		FEATURES.add( TRACK_TOTAL_DISTANCE_TRAVELLED );
		FEATURES.add( TRACK_MAX_DISTANCE_TRAVELLED );
		FEATURES.add( TRACK_CONFINMENT_RATIO );
		FEATURES.add( TRACK_MEAN_STRAIGHT_LINE_SPEED );
		FEATURES.add( TRACK_LINEARITY_OF_FORWARD_PROGRESSION );

		FEATURE_NAMES.put( TRACK_TOTAL_DISTANCE_TRAVELLED, "Total distance travelled" );
		FEATURE_NAMES.put( TRACK_MAX_DISTANCE_TRAVELLED, "Max distance travelled" );
		FEATURE_NAMES.put( TRACK_CONFINMENT_RATIO, "Confinment ratio" );
		FEATURE_NAMES.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, "Mean straight line speed" );
		FEATURE_NAMES.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, "Linearity of forward progression" );

		FEATURE_SHORT_NAMES.put( TRACK_TOTAL_DISTANCE_TRAVELLED, "Total dist." );
		FEATURE_SHORT_NAMES.put( TRACK_MAX_DISTANCE_TRAVELLED, "Max dist." );
		FEATURE_SHORT_NAMES.put( TRACK_CONFINMENT_RATIO, "Cnfnmnt ratio" );
		FEATURE_SHORT_NAMES.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, "Mean v. line" );
		FEATURE_SHORT_NAMES.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, "Lin. fwd. progr." );

		FEATURE_DIMENSIONS.put( TRACK_TOTAL_DISTANCE_TRAVELLED, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( TRACK_MAX_DISTANCE_TRAVELLED, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( TRACK_CONFINMENT_RATIO, Dimension.NONE );
		FEATURE_DIMENSIONS.put( TRACK_MEAN_STRAIGHT_LINE_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_LINEARITY_OF_FORWARD_PROGRESSION, Dimension.NONE );

		IS_INT.put( TRACK_TOTAL_DISTANCE_TRAVELLED, Boolean.FALSE );
		IS_INT.put( TRACK_MAX_DISTANCE_TRAVELLED, Boolean.FALSE );
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
