package runnerstub;

import android.content.Intent;
import android.service.quicksettings.TileService;

public class ExecTileService extends TileService {

    @Override
    public void onClick() {
        super.onClick();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        startService(new Intent(this, ExecService.class));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }
}
