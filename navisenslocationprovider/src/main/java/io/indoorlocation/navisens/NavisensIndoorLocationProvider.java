package io.indoorlocation.navisens;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaApplication;
import com.navisens.motiondnaapi.MotionDnaInterface;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.core.IndoorLocationProviderListener;

import static android.os.SystemClock.elapsedRealtime;


public class NavisensIndoorLocationProvider extends IndoorLocationProvider implements MotionDnaInterface, IndoorLocationProviderListener {
    private Context mContext;

    private MotionDnaApplication mMotionDna;
    private String mNavisensKey;
    private IndoorLocationProvider mSourceProvider;
    private boolean mStarted = false;
    private Double mCurrentFloor = null;

    private int mUpdateRate = 500;
    private MotionDna.PowerConsumptionMode mPowerMode = MotionDna.PowerConsumptionMode.PERFORMANCE;
    Context context;
    public double latitude = 0.0;
    public double longitude = 0.0;

    private String room = "m2a", room2 = "m2a2";
    private String host = "192.168.1.102";
    private String port = "6666";
    private String lat, lon;

    Hashtable<String, MotionDna> networkUsers = new Hashtable<String, MotionDna>();
    Hashtable<String, Double> networkUsersTimestamps = new Hashtable<String, Double>();

    public Map<String, MotionDna> payload;

    public String sharedLoc;
    public String trolleyDeviceID = "";
    /**
     * Create a new instance of Navisens location provider
     * @param context
     * @param navisensKey
     */


    public NavisensIndoorLocationProvider(Context context, String navisensKey) {
        super();
        mContext = context;
        mNavisensKey = navisensKey;
        mMotionDna = new MotionDnaApplication(this);

    }

    /**
     * Create a new instance of Navisens location provider
     * @param context
     * @param navisensKey
     * @param sourceProvider will be use to provide location to navisens
     */
    public NavisensIndoorLocationProvider(Context context, String navisensKey, IndoorLocationProvider sourceProvider) {
        super();
        mContext = context;
        mNavisensKey = navisensKey;
        mSourceProvider = sourceProvider;
        mSourceProvider.addListener(this);
        mMotionDna = new MotionDnaApplication(this);
    }
    public NavisensIndoorLocationProvider(Context context){
        this.context = context;
    }
    /**
     * Get the instance of motion dna used by this provider.
     * It allows you to configure navisens as you want.
     * @return MotionDnaApplication
     */
    public MotionDnaApplication getMotionDna() {
        return mMotionDna;
    }

    /**
     * Manually provide location to navisens
     * @param indoorLocation
     */
    public void setIndoorLocation(IndoorLocation indoorLocation) {

        latitude = indoorLocation.getLatitude();
        longitude = indoorLocation.getLongitude();
        dispatchIndoorLocationChange(indoorLocation);
        mCurrentFloor = indoorLocation.getFloor();
        mMotionDna.setLocationLatitudeLongitude(latitude, longitude);
        mMotionDna.setHeadingMagInDegrees();

        if (mCurrentFloor != null) {
            mMotionDna.setFloorNumber(mCurrentFloor.intValue());
        }
        lat = Double.toString(latitude);
        lon = Double.toString(longitude);
        mMotionDna.sendUDPPacket("Latitude : "+lat +"\nLongitude : "+lon);
    }

    public void setLocFromQR(Double setLat, Double setLon){
        mMotionDna.setLocationLatitudeLongitude(setLat, setLon);
        mMotionDna.setHeadingMagInDegrees();
    }


    @Override
    public boolean supportsFloor() {
        return true;
    }

    @Override
    public void start() {
        if (!mStarted) {
            mStarted = true;
            mMotionDna.runMotionDna(mNavisensKey);
            mMotionDna.setCallbackUpdateRateInMs(mUpdateRate);
            mMotionDna.setPowerMode(mPowerMode);
            mMotionDna.setLocationGPSOnly();
            mMotionDna.setLocationNavisens();
            mMotionDna.startUDP(room, host, port);
//            mMotionDna.setBackpropagationEnabled(true);
            mMotionDna.setNetworkUpdateRateInMs(500);
        }
    }

    public void startingUDP(int choose){
        mMotionDna.stopUDP();
        switch(choose){
            case 1: mMotionDna.startUDP(room, host, port);
            case 2: mMotionDna.startUDP(room2, host, port);
        }
    }

    @Override
    public void stop() {
        if (mStarted) {
            mStarted = false;
            mMotionDna.stop();
        }
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        MotionDna.Location location = motionDna.getLocation();
        mCurrentFloor = (double)location.floor;
        IndoorLocation indoorLocation = new IndoorLocation(getName(), location.globalLocation.latitude, location.globalLocation.longitude, mCurrentFloor, System.currentTimeMillis());
        dispatchIndoorLocationChange(indoorLocation);
        if(indoorLocation.getLatitude()!= 0 && indoorLocation.getLongitude()!=0){
            dispatchIndoorLocationChange(indoorLocation);
        }

    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {
        networkUsers.put(motionDna.getID(),motionDna);
        trolleyDeviceID = motionDna.getID();
        double timeSinceBootSeconds = elapsedRealtime() / 1000.0;
        networkUsersTimestamps.put(motionDna.getID(),timeSinceBootSeconds);
        List<String> toRemove = new ArrayList();

        for (MotionDna user: networkUsers.values()) {
            if (timeSinceBootSeconds - networkUsersTimestamps.get(user.getID()) > 2.0) {
                toRemove.add(user.getID());
            } else {
                MotionDna.GlobalLocation loc = user.getLocation().globalLocation;
                sharedLoc=Double.toString(loc.latitude)+" "+Double.toString(loc.longitude);
            }
        }
        for (String key: toRemove) {
            networkUsers.remove(key);
            networkUsersTimestamps.remove(key);
        }
    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ?> map) {

    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
        this.dispatchOnProviderError(new Error(errorCode.toString() + " " + s));
    }

    @Override
    public Context getAppContext() {
        return mContext.getApplicationContext();
    }

    @Override
    public PackageManager getPkgManager() {
        return mContext.getPackageManager();
    }

    @Override
    public void onProviderStarted() {
        this.dispatchOnProviderStarted();
    }

    @Override
    public void onProviderStopped() {
        this.dispatchOnProviderStopped();
    }

    @Override
    public void onProviderError(Error error) {
       dispatchOnProviderError(error);
    }

    @Override
    public void onIndoorLocationChange(IndoorLocation indoorLocation) {
        setIndoorLocation(indoorLocation);
        dispatchIndoorLocationChange(indoorLocation);
    }
}
