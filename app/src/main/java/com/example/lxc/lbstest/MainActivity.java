package com.example.lxc.lbstest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;
    private TextView positonText;
    private TextureMapView mapView;

    private BaiduMap baiduMap;
    private boolean isFirstLocate=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationClient.setAgreePrivacy(true);//定位
        try {
            mLocationClient = new LocationClient(getApplicationContext());
            mLocationClient.registerLocationListener(new MyLocationListener());
        }catch (Exception e){

        }

        SDKInitializer.setAgreePrivacy(getApplicationContext(), true); //地图
        try {
            //在使用SDK各组件之前初始化context信息，传入ApplicationContext
            SDKInitializer.initialize(getApplicationContext());
            //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
            //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
            SDKInitializer.setCoordType(CoordType.BD09LL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        mapView = (TextureMapView) findViewById(R.id.bmapview);
        positonText = (TextView) findViewById(R.id.position_text);
        baiduMap = mapView.getMap();//地图控制
        //普通地图 ,BaiduMap是地图控制器对象
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setMyLocationEnabled(true);//我的位置


        if (hasRequiredPermission(this, PERMISSIONS_LIST, 1)) {
            requestLocation();
        }

        /**List<String> permissionList = new ArrayList<>();
         //判断是否授过权
         if(ContextCompat.checkSelfPermission(LbsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
         != PackageManager.PERMISSION_GRANTED) {
         permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
         }
         if(ContextCompat.checkSelfPermission(LbsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
         != PackageManager.PERMISSION_GRANTED) {
         permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
         }
         if(ContextCompat.checkSelfPermission(LbsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
         != PackageManager.PERMISSION_GRANTED) {
         permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
         }
         if(!permissionList.isEmpty()){
         String [] permissions = permissionList.toArray(new String[permissionList.size()]);
         ActivityCompat.requestPermissions(LbsActivity.this,permissions,1);
         }else {
         requestLocation();
         }*/
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(1000);
        option.setCoorType("BD09LL"); // 设置坐标类型
        // 打开gps
        option.setOpenGps(true);
        //option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);//强制GPS
        option.setIsNeedAddress(true);//获取详细地址信息
        mLocationClient.setLocOption(option);
    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();//定位开始
    }


    public static final String[] PERMISSIONS_LIST = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
    };

    public static boolean hasRequiredPermission(final Activity activity, final String[] permissions, final int requestCode) {

        ArrayList<String> absenceSet = new ArrayList<String>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                absenceSet.add(permission);
            }
        }

        if (!absenceSet.isEmpty()) {
            String[] requestArray = new String[absenceSet.size()];
            absenceSet.toArray(requestArray);

            ActivityCompat.requestPermissions(activity, requestArray, requestCode);
            return false;
        }
        return true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"你需要打开权限",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this,"你需要打开权限",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    //地图控制
    private  void navigateTo(BDLocation location){
        if(isFirstLocate){
            LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(18f);
            baiduMap.animateMapStatus(update);
            isFirstLocate =false;

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            baiduMap.setMyLocationData(locData);
        }

    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {

            // MapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }

            if(location.getLocType() == BDLocation.TypeNetWorkLocation || location.getLocType() == BDLocation.TypeGpsLocation){
                navigateTo(location);
            }

            StringBuilder currentlocation = new StringBuilder();
            currentlocation.append("纬度:").append(location.getLatitude()).append("\n");
            currentlocation.append("经线:").append(location.getLongitude()).append("\n");
            currentlocation.append("国家:").append(location.getCountry()).append("\n");
            currentlocation.append("省:").append(location.getProvince()).append("\n");
            currentlocation.append("市:").append(location.getCity()).append("\n");
            currentlocation.append("区:").append(location.getDistrict()).append("\n");
            currentlocation.append("街道:").append(location.getStreet()).append("\n");

            currentlocation.append("定位方式:");
            android.util.Log.d("lxc","location.getLocType()="+location.getLocType());
            if(location.getLocType() == BDLocation.TypeGpsLocation){
                currentlocation.append("GPS");
            } else if(location.getLocType() == BDLocation.TypeNetWorkLocation){
                currentlocation.append("网络");
            }
            positonText.setText(currentlocation);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        mapView = null;
        baiduMap.setMyLocationEnabled(false);
    }
}
