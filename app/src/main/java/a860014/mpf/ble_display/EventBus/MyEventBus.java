package a860014.mpf.ble_display.EventBus;

import org.greenrobot.eventbus.EventBus;

import a860014.mpf.ble_display.MyEventBusIndex;

/**
 * Created by letian on 6/25/17.
 */

public class MyEventBus {

    public final static EventBus eventBus = EventBus.builder().addIndex(new MyEventBusIndex()).build();

}
