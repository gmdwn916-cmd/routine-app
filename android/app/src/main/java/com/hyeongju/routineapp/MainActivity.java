package com.hyeongju.routineapp;

import android.os.Bundle;
import android.webkit.WebSettings;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetBridgePlugin.class);
        super.onCreate(savedInstanceState);
        // 개발 중 계속 새 APK를 설치해서 테스트하는데, 안드로이드는 APK를 새로
        // 설치해도 앱 데이터/캐시는 그대로 유지해서 웹뷰가 예전 index.html을
        // 계속 캐시해서 보여주는 문제가 있었음(위젯 데이터 형식이 계속 옛날
        // 방식으로 오던 원인이 이것). 캐시를 아예 안 쓰게 해서 항상 방금 설치한
        // 최신 코드로 실행되게 함.
        this.bridge.getWebView().getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    }
}
