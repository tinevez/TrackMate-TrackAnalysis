package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class LinearTrackDescriptorTest
{

	private Model model;

	@Before
	public void setUp()
	{
		model = new Model();
		Settings settings = new Settings();
		// NB: populate TrackAnalyzers in the order of SciJava priority
		populateTrackAnalyzers(settings);
		new ModelFeatureUpdater( model, settings );

		model.beginUpdate();
		double[][] track1points = new double[][] { { 1, 1, 0 }, { 1, 2, 0 }, { 1, 3, 0 }, { 1, 4, 0 }, { 1, 5, 0 } };
		createTrack( track1points );
		double[][] track2points = new double[][] { { 4, 2, 0 }, { 6, 2, 0 }, { 8, 2, 0 }, { 9, 2, 0 }, { 10, 8, 0 } };
		createTrack( track2points );
		model.endUpdate();
	}

	private void populateTrackAnalyzers( Settings s )
	{
		TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
		for ( String key : trackAnalyzerProvider.getKeys() )
		{
			s.addTrackAnalyzer( trackAnalyzerProvider.getFactory( key ) );
		}
	}

	private void createTrack( double[][] points )
	{
		Spot s1 = null;
		Spot s2 = null;
		int frame = 0;
		for ( double[] p : points )
		{
			s2 = new Spot( p[ 0 ], p[ 1 ], p[ 2 ], 1.0, 1.0 );
			s2.putFeature( Spot.POSITION_T, ( double ) frame );
			model.addSpotTo( s2, frame++ );
			if ( s1 == null )
			{
				s1 = s2;
				continue;
			}
			model.addEdge( s1, s2, -1 );
			s1 = s2;
		}
	}

	@Test
	public void test()
	{
		assertEquals( 5, model.getSpots().keySet().size() );
		assertEquals( 2, model.getTrackModel().nTracks( true ) );

		FeatureModel featureModel = model.getFeatureModel();

		assertEquals( 90, Math.toDegrees( featureModel.getTrackFeature( 0, LinearTrackDescriptor.TOTAL_ABSOLUTE_ANGLE_XY ) ), 0.0 );
		assertEquals( 45, Math.toDegrees( featureModel.getTrackFeature( 1, LinearTrackDescriptor.TOTAL_ABSOLUTE_ANGLE_XY ) ), 0.0 );
	}
}
