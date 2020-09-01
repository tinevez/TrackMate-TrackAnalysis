package fiji.plugin.trackmate.features.track;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;

public class LinearTrackDescriptorTestDrive
{

	public static void main( final String[] args )
	{
		final ImagePlus imp = IJ.openImage( "https://samples.fiji.sc/FakeTracks.tif" );

		final Settings settings = new Settings();
		settings.setFrom( imp );
		settings.addAllAnalyzers();
		settings.defaultParameters();
		settings.addTrackFilter( new FeatureFilter( TrackDurationAnalyzer.TRACK_DISPLACEMENT, 10., true ) );

		final TrackMate trackmate = new TrackMate( settings );
		if ( !trackmate.checkInput() || !trackmate.process() )
		{
			System.err.println( trackmate.getErrorMessage() );
			return;
		}

		System.out.println( "Tracking complete." );

		final Model model = trackmate.getModel();
		final SelectionModel selectionModel = new SelectionModel( model );

		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, selectionModel, settings.imp );
		displayer.render();

		new ExportStatsToIJAction( selectionModel ).execute( trackmate );

	}

}
