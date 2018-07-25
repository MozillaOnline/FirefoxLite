package org.mozilla.focus.content;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StyleRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.tabs.tabtray.InterceptBehavior;
import android.view.GestureDetector.SimpleOnGestureListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mozillabeijing on 2018/8/9.
 */

public class ContentTrayFragment extends DialogFragment {

    private RecyclerView contentListView;
    private ContentAdapter mAdapter;
    private int PAGE_COUNT = 5;
    private LinearLayoutManager mLayoutManager;
    private Context context;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private static final float OVERLAY_ALPHA_FULL_EXPANDED = 0.50f;

    private static final boolean ENABLE_BACKGROUND_ALPHA_TRANSITION = true;

    private View backgroundView;
    private Drawable backgroundDrawable;
    private Drawable backgroundOverlay;

    private SlideAnimationCoordinator slideCoordinator = new SlideAnimationCoordinator(this);

    private boolean playEnterAnimation = true;

    private List<Content> data = new ArrayList<>();
    private int lastVisibleItem = 0;

    public static final String FRAGMENT_TAG = "content_tray";

    public static ContentTrayFragment newInstance() {
        return new ContentTrayFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.TabTrayTheme);
        context = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (playEnterAnimation) {
            playEnterAnimation = false;
            setDialogAnimation(R.style.TabTrayDialogEnterExit);

        } else {
            setDialogAnimation(R.style.TabTrayDialogExit);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_displaycontent, container, false);
        backgroundView = view.findViewById(R.id.content_root_view);
        contentListView = (RecyclerView) view.findViewById(R.id.content_recyclerview);
        contentListView.setVisibility(View.VISIBLE);
        contentListView.setLayoutManager(mLayoutManager);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initWindowBackground(view.getContext());

        setupBottomSheetCallback();

        prepareExpandAnimation();

        initRecyclerViewStyle(contentListView);

        setupTapBackgroundToExpand();
        initData();

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                startExpandAnimation();
                return false;
            }
        });
    }

    private void initData(){
        JSONArray obj = null;
        new ContentTrayFragment.AsyncTaskSendRequest(obj).execute();
        Log.e("DisplayContent","execute");
        mLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        Log.e("DisplayContent","mLayoutManager");
        new ContentTrayFragment.AsyncTaskUpdate(obj).execute();
    }

    private List<Content> getData(JSONArray jsonArray) {
        Log.e("DisplayContent","getData");

        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json_content = (JSONObject)jsonArray.get(i);
                    final Content content = new Content();
                    String imgUrl = json_content.getJSONArray("image").get(0).toString();
                    imgUrl.replace("\\","");
                    Log.e("DisplayContent",imgUrl);
                    content.setTitle(json_content.getString("title"));
                    content.setUrl(json_content.getString("url"));
                    content.setContent(json_content.getString("summary"));
                    content.setImgUrl(imgUrl);
                    data.add(content);
                }
            }
        }  catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return data;
        }
    }

    private JSONArray sendRequest(){
        Log.e("DisplayContent","sendRequest");
        JSONArray arr = new JSONArray();
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL("http://api.tepintehui.com/index.php?m=firefox&c=article&a=get_article&token=a60a09b9ecaf074bc1cf657a75e7007e");
            //URL url = new URL("https://m.g-fox.cn/cnrocket.gif?clientID=526a8ef8-df23-4afc-812a-964177de67a2&device=HUAWEI-HUAWEI+NXT-AL10&type=start&documentID=ef3890df-175b-4743-b45b-2e9dcbb0ef90&version=2.3.0beta");
            connection = ((HttpURLConnection) url.openConnection());
            Log.e("DisplayContent","openConnection");
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            Log.e("DisplayContent","settings for connection");

            int response = connection.getResponseCode();
            Log.e("DisplayContent", String.valueOf(response));

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            Log.e("DisplayContent","connect");
            //StringBuilder responseStrBuilder = new StringBuilder();
            //JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String inputStr;
            /*while ((inputStr = in.readLine()) != null){
                Log.e("DisplayContent", inputStr);
                responseStrBuilder.append(inputStr);
            }*/
            inputStr = in.readLine();
            String str;
            str = inputStr.replace("\\","");
            str = str.substring(2);
            Log.e("DisplayContent",str);
            String [] strArr = str.split(",\\{\"t");
            Log.e("DisplayContent",strArr[0]);
            str = strArr[0];
            if(str.endsWith(",")){
                str =str.substring(0,str.length()-2);
            }
            Log.e("DisplayContent",str);
            try{
                JSONObject jsonObject = new JSONObject(str);
                Log.e("DisplayContent","jsonobj");
                arr = jsonObject.getJSONArray("data");
                Log.e("DisplayContent","arr");
                Log.e("DisplayContent", arr.get(0).toString());
            }catch (Exception e){
                e.printStackTrace();
                Log.e("Displaycontent","jsonObject not create");
            }


        }catch (MalformedURLException var11) {
            Log.e("DisplayContent", "Could not upload telemetry due to malformed URL", var11);
            var11.printStackTrace();
        } catch (IOException var12) {
            Log.w("DisplayContent", "IOException while uploading ping", var12);
            var12.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            if(connection != null) {
                connection.disconnect();
            }
            return arr;
        }

    }

    private  class AsyncTaskUpdate extends android.os.AsyncTask<String,String,JSONArray>{
        JSONArray jsonArray;
        public AsyncTaskUpdate(JSONArray obj){this.jsonArray = obj;}

        @Override
        protected JSONArray doInBackground(String... params){
            return sendRequest();
        }
        @Override
        protected void onPostExecute(JSONArray obj){
            contentListView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    // 在newState为滑到底部时
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateRecyclerView(obj);
                    }
                }
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // 在滑动完成后，拿到最后一个可见的item的位置
                    lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                }
            });

            jsonArray = obj;
        }
    }
    private class AsyncTaskSendRequest extends android.os.AsyncTask<String,String,JSONArray> {
        JSONArray jsonArray;
        public AsyncTaskSendRequest(JSONArray obj){this.jsonArray = obj;}
        @Override
        protected JSONArray doInBackground(String... params){
            return sendRequest();
        }
        @Override
        protected void onPostExecute(JSONArray obj){
            Log.e("DisplayContent","onPost");
            try {
                Log.e("DisplayContent", obj.get(0).toString());
            }catch (Exception e){
                e.printStackTrace();
                Log.e("DisplayContent","cannot fetch obj(0)");
                new ContentTrayFragment.AsyncTaskUpdate(obj).execute();

            }
            mAdapter = new ContentAdapter(getData(obj), context);
            contentListView.setAdapter(mAdapter);
            if(mAdapter == null){
                Log.e("DisplayContent","mAdapter is null");
            }

            mAdapter.setOnItemClickListener(new ContentAdapter.OnItemClickListener(){
                @Override
                public void onItemClick(View view , int position){
                    if (position != RecyclerView.NO_POSITION && position < mAdapter.getItemCount()) {
                        Content item = mAdapter.mItems.get(position);
                        Log.e("DisplayContent",item.getUrl());
                        ScreenNavigator.get(context).showBrowserScreen(item.getUrl(),true,false);
                        dismiss();
                    }
                }
            });


            jsonArray = obj;
        }
    }

    private void updateRecyclerView(JSONArray obj) {
        // 获取从fromIndex到toIndex的数据
        List<Content> newData = getData(obj);
        if (newData.size() > 0) {
            // 然后传给Adapter，并设置hasMore为true
            mAdapter.updateList(newData);
        } else {
            mAdapter.updateList(null);
        }
    }

    private void initWindowBackground(Context context) {
        Drawable drawable = context.getDrawable(R.drawable.content_tray_background);
        if (drawable == null) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("fail to resolve background drawable");
            }
            return;
        }

        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.content_background);
            backgroundOverlay = layerDrawable.findDrawableByLayerId(R.id.content_background_overlay);
            int alpha = validateBackgroundAlpha(0xff);
            backgroundDrawable.setAlpha(alpha);
            backgroundOverlay.setAlpha(getBottomSheetState() == BottomSheetBehavior.STATE_COLLAPSED ? 0 : (int) (alpha * OVERLAY_ALPHA_FULL_EXPANDED));

        } else {
            backgroundDrawable = drawable;
        }

        Window window = getDialog().getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(drawable);
        Log.e("displaycontent","setbackground drawable");
    }

    private void setDialogAnimation(@StyleRes int resId) {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = resId;
            updateWindowAttrs(window);
        }
    }

    private void updateWindowAttrs(@NonNull Window window) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return;
        }

        View decor = window.getDecorView();
        if (decor.isAttachedToWindow()) {
            manager.updateViewLayout(decor, window.getAttributes());
        }
    }

    private int validateBackgroundAlpha(int alpha) {
        return Math.max(Math.min(alpha, 0xfe), 0x01);
    }

    private int getBottomSheetState() {
        BottomSheetBehavior behavior = getBehavior(contentListView);
        if (behavior != null) {
            return behavior.getState();
        }
        return -1;
    }

    @Nullable
    private InterceptBehavior getBehavior(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            return null;
        }

        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (behavior == null) {
            return null;
        }

        if (behavior instanceof InterceptBehavior) {
            return (InterceptBehavior) behavior;
        }
        return null;
    }

    private static class SlideAnimationCoordinator {
        private Interpolator backgroundInterpolator = new AccelerateInterpolator();
        private Interpolator overlayInterpolator = new AccelerateDecelerateInterpolator();
        private int collapseHeight = -1;

        private float translationY = Integer.MIN_VALUE;
        private float backgroundAlpha = -1;
        private float overlayAlpha = -1;

        private ContentTrayFragment fragment;

        SlideAnimationCoordinator(ContentTrayFragment fragment) {
            this.fragment = fragment;
        }

        private void onSlide(float slideOffset) {
            float backgroundAlpha = 1f;
            float overlayAlpha = 0f;

            float translationY = 0;

            if (slideOffset < 0) {
                if (collapseHeight < 0) {
                    collapseHeight = fragment.getCollapseHeight();
                }
                translationY = collapseHeight * -slideOffset;

                if (ENABLE_BACKGROUND_ALPHA_TRANSITION) {
                    float interpolated = backgroundInterpolator.getInterpolation(-slideOffset);
                    backgroundAlpha = Math.max(0, 1 - interpolated);
                }
            } else {
                float interpolated = overlayInterpolator.getInterpolation(1 - slideOffset);
                overlayAlpha = -(interpolated * OVERLAY_ALPHA_FULL_EXPANDED) + OVERLAY_ALPHA_FULL_EXPANDED;
            }

            if (slideOffset >= 1) {
                fragment.onFullyExpanded();
            }

            if (Float.compare(this.translationY, translationY) != 0) {
                this.translationY = translationY;
                //fragment.onTranslateToHidden(translationY);
            }

            if (Float.compare(this.backgroundAlpha, backgroundAlpha) != 0) {
                this.backgroundAlpha = backgroundAlpha;
                fragment.updateWindowBackground(backgroundAlpha);
            }

            if (Float.compare(this.overlayAlpha, overlayAlpha) != 0) {
                this.overlayAlpha = overlayAlpha;
                fragment.updateWindowOverlay(overlayAlpha);
            }
        }
    }

    private void onFullyExpanded() {
        setIntercept(false);
    }


    private void updateWindowBackground(float backgroundAlpha) {
        backgroundView.setAlpha(backgroundAlpha);

        if (backgroundDrawable != null) {
            //backgroundDrawable.setAlpha(validateBackgroundAlpha((int) (backgroundAlpha * 0xff)));
            backgroundDrawable.setAlpha(192);
        }
    }

    private void updateWindowOverlay(float overlayAlpha) {
        if (backgroundOverlay != null) {
            backgroundOverlay.setAlpha(validateBackgroundAlpha((int) (overlayAlpha * 0xff)));
        }
    }

    private int getCollapseHeight() {
        BottomSheetBehavior behavior = getBehavior(contentListView);
        if (behavior != null) {
            return behavior.getPeekHeight();
        }
        return 0;
    }

    private void initRecyclerViewStyle(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(mLayoutManager = new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false));

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    private void prepareExpandAnimation() {
        setBottomSheetState(BottomSheetBehavior.STATE_HIDDEN);

        // update logo-man and background alpha state
        //slideCoordinator.onSlide(-1);
        slideCoordinator.onSlide(1);
        //logoMan.setVisibility(View.INVISIBLE);
    }

    private void setupTapBackgroundToExpand() {
        final GestureDetectorCompat detector = new GestureDetectorCompat(getContext(),
                new SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                });

        backgroundView.setOnTouchListener((v, event) -> {
            boolean result = detector.onTouchEvent(event);
            if (result) {
                v.performClick();
            }
            return result;
        });
    }

    private void startExpandAnimation() {
        uiHandler.postDelayed(() -> {
                //setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED);
            setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                //logoMan.setVisibility(View.VISIBLE);
                setIntercept(false);
        }, getResources().getInteger(R.integer.tab_tray_transition_time));
    }

    private void setBottomSheetState(@BottomSheetBehavior.State int state) {
        BottomSheetBehavior behavior = getBehavior(contentListView);
        if (behavior != null) {
            behavior.setState(state);
        }
    }
    private void setIntercept(boolean intercept) {
        InterceptBehavior behavior = getBehavior(contentListView);
        if (behavior != null) {
            behavior.setIntercept(intercept);
        }
    }
    private void setupBottomSheetCallback() {
        BottomSheetBehavior behavior = getBehavior(contentListView);
        if (behavior == null) {
            return;
        }

        behavior.setBottomSheetCallback(new BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                slideCoordinator.onSlide(slideOffset);
            }
        });
    }
}
