package com.smartfoxitsolutions.lockup;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by RAAJA  for SmartFoxItSolutions on 28-08-2016.
 *
 * View which draws a 3X3 matrix of square nodes and handles the pattern drawn from touch events on the nodes.
 * Suitable for pattern locks.
 */
public class PatternLockView extends View {

    private float nodeRectSize,nodeCornerSize, nodeSelectedCornerSize, patternLineWidth;
    private int patternViewDimension;
    private String[] nodeColor,nodeSelectedColor;
    private int nodeDefaultColor, nodeDefaultSelectedColor,patternLineColor;
    private boolean patternError, drawPattern;
    private Paint nodePaint,linePaint;
    private ArrayList<Node> nodeList;
    private OnPatternChangedListener patternListener;
    private float prevNodeX, prevNodeY, currentMovementX, currentMovementY;
    private Path patternPath;
    private PathMeasure pathMeasure;
    private float[] pathCentreCoordinates = {0f, 0f};


    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPatternView(context,attrs);
    }

    /**
     * Initializes the PatternLockView from the Context and the AttributeSet parameters.
     *
     * @param context The Context in which the view is running
     * @param attrs The AttributeSet for view's attribute which is set in XML
     */

    private void initPatternView(Context context,AttributeSet attrs){
        TypedArray attributeArray = context.obtainStyledAttributes(attrs,R.styleable.PatternLockView,0,0);
        float nodeRectSizePixel = attributeArray.getDimension(R.styleable.PatternLockView_nodeCellSize,30);
        float nodeCornerSizePixel = attributeArray.getDimension(R.styleable.PatternLockView_nodeCornerSize,5);
        float nodeSelectedCornerPixel = attributeArray.getDimension(R.styleable.PatternLockView_nodeSelectedCornerSize,7);
        float patternStrokeWidthPixel = attributeArray.getDimension(R.styleable.PatternLockView_patternLineStroke,10);
        int patternLineColorID = attributeArray.getColor(R.styleable.PatternLockView_patternLineColor,Color.BLUE);
        int nodeDefaultColorID = attributeArray.getColor(R.styleable.PatternLockView_nodeDefaultColor,Color.GREEN);
        int nodeSelectedDefaultColorID=attributeArray.getColor(R.styleable.PatternLockView_nodeSelectedDefaultColor,Color.RED);
        int nodeColorResourceID = attributeArray.getResourceId(R.styleable.PatternLockView_nodeColor,0);
        int nodeSelectedColorResourceID = attributeArray.getResourceId(R.styleable.PatternLockView_nodeSelectedColor,0);
        attributeArray.recycle();

        setNodeCornerSize(nodeCornerSizePixel);
        setNodeRectSize(nodeRectSizePixel);
        setNodeSelectedCornerSize(nodeSelectedCornerPixel);
        setPatternLineWidth(patternStrokeWidthPixel);
        setNodeDefaultColor(nodeDefaultColorID);
        setPatternLineColor(patternLineColorID);
        setNodeDefaultSelectedColor(nodeSelectedDefaultColorID);
        if(nodeColorResourceID !=0){
           String[] colorResource =  getResources().getStringArray(nodeColorResourceID);
            setNodeColor(colorResource);
        }
        if (nodeSelectedColorResourceID !=0){
            String[] colorSelectedResource = getResources().getStringArray(nodeSelectedColorResourceID);
            setNodeSelectedColor(colorSelectedResource);
        }
        setPatternPath();
        setPathMeasure();
        setNodePaint();
        setLinePaint();
        initNodes();
    }

    /** ---------------- Setters And Getters ------------------*/
                // The rect size in which the nodes are drawn.
    public void setNodeRectSize(float nodeRectPixel){
        this.nodeRectSize = nodeRectPixel;
    }

    public void setNodeCornerSize(float nodeCornerPixel){
        this.nodeCornerSize = nodeCornerPixel;
    }

    public void setPatternLineWidth(float patternLineWidth){
        this.patternLineWidth = patternLineWidth;
    }

    public void setNodeSelectedCornerSize(float nodeSelectedCorner){
        this.nodeSelectedCornerSize = nodeSelectedCorner;
    }

                /* Sets the total dimension of the Pattern View without padding after the measurement
                *  in {@code onMeasure()} method.
                * */
    public void setPatternViewDimension(int measuredPatternDimension){
        this.patternViewDimension = measuredPatternDimension;
    }

    public void setPatternPath(){
        this.patternPath = new Path();
    }

    public void setNodeColor(String[] colorArray){
        if((colorArray.length ==9)) {
            this.nodeColor = colorArray;
        }
    }

    public void setNodeSelectedColor(String[] colorSelectedArray){
        if((colorSelectedArray.length==9)){
            this.nodeSelectedColor = colorSelectedArray;
        }
    }

    public void setPatternLineColor(int color){
        this.patternLineColor = color;
    }

    public void setNodeDefaultColor(int color){
        this.nodeDefaultColor = color;
    }

    public void setNodeDefaultSelectedColor(int color){
        this.nodeDefaultSelectedColor =color;
    }

    public void setPrevNodeX(float prevX){
        this.prevNodeX = prevX;
    }

    public void setPrevNodeY(float prevY){
        this.prevNodeY = prevY;
    }

    public void setCurrentMovementX(float currentX){
        this.currentMovementX = currentX;
    }

    public void setCurrentMovementY(float currentY){
        this.currentMovementY = currentY;
    }

                    //Sets a boolean flag to listen for after the pattern is completed. If false touch events are handled.
    public void setPatternError(boolean errorPattern){
        this.patternError = errorPattern;
    }
    /** Sets the flag for drawing pattern line. As the touch is detected from every point inside
     * the pattern view, setting this flag will draw the pattern line only when the patter is started*/
    private void setDrawPattern(boolean drawPattern){this.drawPattern = drawPattern;}

    private void setNodeList(ArrayList<Node> nodeList){
        this.nodeList = nodeList;
    }

    public void setOnPatternChangedListener(OnPatternChangedListener patternListener){
        this.patternListener = patternListener;
    }

    public void setNodePaint(){
        this.nodePaint = new Paint();
        nodePaint.setStyle(Paint.Style.FILL);
        nodePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    public void setLinePaint(){
        this.linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(getPatternLineWidth());
        linePaint.setColor(getPatternLineColor());
        linePaint.setAlpha(125);
    }

    private void setPathMeasure(){
        this.pathMeasure = new PathMeasure(getPatternPath(),false);
    }

    public float getNodeRectSize(){
        return this.nodeRectSize;
    }

    public  float getNodeCornerSize(){
        return this.nodeCornerSize;
    }

    public float getNodeSelectedCornerSize(){
        return this.nodeSelectedCornerSize;
    }

    public float getPatternLineWidth(){
        return this.patternLineWidth;
    }

    public int getPatternViewDimension(){
        return this.patternViewDimension;
    }

    public Path getPatternPath(){
        if(patternPath==null){
            return new Path();
        }else{
            return this.patternPath;
        }
    }

    public String[] getNodeColor(){
        return this.nodeColor;
    }

    public String[] getNodeSelectedColor(){
        return this.nodeSelectedColor;
    }

    public int getPatternLineColor(){
        return this.patternLineColor;
    }

    public int getNodeDefaultColor(){
        return this.nodeDefaultColor;
    }

    public int getNodeDefaultSelectedColor(){
        return this.nodeDefaultSelectedColor;
    }

    public float getPrevNodeX(){
        return this.prevNodeX;
    }

    public float getPrevNodeY(){
        return this.prevNodeY;
    }

    public float getCurrentMovementX(){
        return this.currentMovementX;
    }

    public float getCurrentMovementY(){
        return this.currentMovementY;
    }

    public boolean isPatternError(){
        return this.patternError;
    }

    private boolean isDrawPattern(){return this.drawPattern;}

    private ArrayList<Node> getNodeList(){
        return this.nodeList;
    }

    public OnPatternChangedListener getOnPatternChangedListener(){
        return this.patternListener;
    }

    public Paint getNodePaint(){
        return this.nodePaint;
    }

    public Paint getLinePaint(){
        return this.linePaint;
    }

    private PathMeasure getPathMeasure(){
        return this.pathMeasure;
    }


    /* ------------ End of Setters And Getters ------------- */

    /**
     * Listener interface which should be implemented to listen for changes in pattern
     */
    interface OnPatternChangedListener {

        /**
         * Called when a node is selected from the users's touch movement.
         * @param selectedPatternNode The integer for the selected node number
         */
        void onPatternNodeSelected(int selectedPatternNode);

        /**
         * Called when the user's touch movement is completed, notifying that the pattern is finished.
         * @param patternCompleted The boolean value notifying the pattern completion
         */
        void onPatternCompleted(boolean patternCompleted);
    }

    /**
     * Class which contains drawing information of a particular node in the Pattern View
     */

   private static class Node{
        private RectF nodeTotalRect; /* Rect for the region around the node*/
        private RectF nodeRect,nodeSelectedRect; /* Rect for the node and selected node for pre Lollipop devices*/
        private float nodeLeft,nodeRight,nodeTop,nodeBottom; /* Size for the node on post Lollipop devices */
        private float nodeSelectedLeft, nodeSelectedRight,nodeSelectedTop,nodeSelectedBottom; /* Size for the selected node on post Lollipop devices */
        private int nodeColor;
        private int nodeSelectedColor;
        private ValueAnimator animator;
        private int nodeInt; /* Identifier of the node drawn from 1-9. Used to identify a particular node for pattern validation */
        private boolean nodeSelected;
        private void setNodeSelected(boolean isSelected){
            this.nodeSelected = isSelected;
        }
        private boolean isNodeSelected(){
            return this.nodeSelected;
        }
        private Node(){}

    }

    /**
     * Initializes the nodes for Pattern View
     */

    private void initNodes(){
        ArrayList<Node> nodeList = new ArrayList<>(9);
        int colorIndex =0;
        String[] nodeColorArray = getNodeColor();
        String[] nodeSelectedColorArray = getNodeSelectedColor();

        for(int i=0;i<3;i++){
            for (int j=0; j<3;j++) {
                Node node = new Node();
                if (nodeColorArray != null && nodeSelectedColorArray != null) {
                    if (nodeColorArray.length == 9 && nodeSelectedColorArray.length == 9) {
                        node.nodeColor = Color.parseColor(nodeColorArray[colorIndex]);
                        node.nodeSelectedColor = Color.parseColor(nodeSelectedColorArray[colorIndex]);
                    }
                    node.nodeInt = ++colorIndex;
                    nodeList.add(node);
                    setNodeAnimator(node);
                }else{
                    node.nodeColor = getNodeDefaultColor();
                    node.nodeSelectedColor = getNodeDefaultSelectedColor();
                    node.nodeInt = ++colorIndex;
                    nodeList.add(node);
                    setNodeAnimator(node);
                }
            }
        }
        setNodeList(nodeList);
        resetIsNodeSelected();
    }

    /**
     * Measures each node to be drawn for Pattern View
     */

   private void measureAndSetNodes(){
        int measuredTotalPatternWidth = getPatternViewDimension()+getPaddingLeft()+getPaddingRight();
        int measuredTotalPatternHeight = getPatternViewDimension()+getPaddingTop()+getPaddingBottom();
        Rect measuredPatternViewRect = new Rect(getPaddingLeft(),getPaddingTop(),measuredTotalPatternWidth-getPaddingRight()
                ,measuredTotalPatternHeight-getPaddingBottom());
        float nodeTotalRectSize = getNodeRectSize(); //Total size inside which a node is drawn
        float nodeSize = nodeTotalRectSize * 0.400f; //node size decreases as the float value increases
        float nodeSelectedSize = nodeTotalRectSize * 0.350f; //node selected size decreases as the float value increases
        float nodeSpace = (measuredPatternViewRect.width()-(nodeTotalRectSize*3))/2;
        ArrayList<Node> nodes = getNodeList();
        int nodeIndex=0;
        for(int i=0;i<3;i++){
            for(int j=0;j<3;j++){
                Node node = nodes.get(nodeIndex++);
                float rectLeft = (nodeSpace*j)+(nodeTotalRectSize*j)+measuredPatternViewRect.left;
                float rectRight = rectLeft+nodeTotalRectSize;
                float rectTop = (nodeSpace*i)+(nodeTotalRectSize*i)+measuredPatternViewRect.top;
                float rectBottom = rectTop+nodeTotalRectSize;
                node.nodeTotalRect = new RectF(rectLeft,rectTop,rectRight,rectBottom);

                node.nodeLeft = node.nodeTotalRect.left + (nodeSize);
                node.nodeRight = node.nodeTotalRect.right - (nodeSize);
                node.nodeTop = node.nodeTotalRect.top + (nodeSize);
                node.nodeBottom = node.nodeTotalRect.bottom - (nodeSize);

                node.nodeSelectedLeft = node.nodeTotalRect.left + nodeSelectedSize;
                node.nodeSelectedRight = node.nodeTotalRect.right - nodeSelectedSize;
                node.nodeSelectedTop = node.nodeTotalRect.top + nodeSelectedSize;
                node.nodeSelectedBottom = node.nodeTotalRect.bottom - nodeSelectedSize;

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    node.nodeRect = new RectF(node.nodeLeft,node.nodeTop,node.nodeRight,node.nodeBottom);
                    node.nodeSelectedRect = new RectF(node.nodeSelectedLeft,node.nodeSelectedTop,node.nodeSelectedRight,node.nodeSelectedBottom);
                }
            }
        }
    }

    private void setNodeAnimator(Node node){
        final Node finalNode = node;
        final float initialAnimationSize = getNodeRectSize()*0.400f; //node size decreases as the float value increases
        final float finalAnimationSize = getNodeRectSize()*0.350f; //node selected size decreases as the float value increases
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            ValueAnimator bounceUp = ValueAnimator.ofFloat(initialAnimationSize, finalAnimationSize);
            bounceUp.setDuration(100);
            bounceUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    finalNode.nodeSelectedLeft = finalNode.nodeTotalRect.left+animatedValue;
                    finalNode.nodeSelectedRight = finalNode.nodeTotalRect.right-animatedValue ;
                    finalNode.nodeSelectedTop = finalNode.nodeTotalRect.top+animatedValue;
                    finalNode.nodeSelectedBottom = finalNode.nodeTotalRect.bottom-animatedValue ;
                    finalNode.nodeSelectedRect.set(finalNode.nodeSelectedLeft,finalNode.nodeSelectedTop
                                                    ,finalNode.nodeSelectedRight,finalNode.nodeSelectedBottom);
                    invalidate();
                }
            });
            node.animator = bounceUp;
        }else {
            ValueAnimator bounceUp = ValueAnimator.ofFloat(initialAnimationSize, finalAnimationSize);
            bounceUp.setDuration(100);
            bounceUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animatedValue = (float) animation.getAnimatedValue();
                    finalNode.nodeSelectedLeft = finalNode.nodeTotalRect.left+animatedValue;
                    finalNode.nodeSelectedRight = finalNode.nodeTotalRect.right-animatedValue ;
                    finalNode.nodeSelectedTop = finalNode.nodeTotalRect.top+animatedValue;
                    finalNode.nodeSelectedBottom = finalNode.nodeTotalRect.bottom-animatedValue ;
                    invalidate();
                }
            });
            node.animator=bounceUp;
        }
    }

    /**
     * Resets the selected nodes to the unselected state
     */

    private void resetIsNodeSelected(){
        for(Node node:getNodeList()){
            node.setNodeSelected(false);
        }
    }

    /**
     * Resets the PatternView to its original state
     */

   public void resetPatternView(){
        resetIsNodeSelected();
        setPatternError(false);
        setDrawPattern(false);
        getPatternPath().rewind();
       getLinePaint().setColor(getPatternLineColor());
       getLinePaint().setAlpha(125);
        invalidate();
    }

    /**
     * Sets the Pattern in Error state. Touch events are not handled.
     */
    public void postPatternError(){
        setPatternError(true);
        getLinePaint().setColor(Color.parseColor("#ef5350"));
        getLinePaint().setAlpha(125);
        invalidate();
    }

    private void startNodeSelectedAnimation(Node node){
        node.animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
        int suggestedWidth = (MeasureSpec.getSize(widthMeasureSpec)-getPaddingLeft())- getPaddingRight();
        int suggestedHeight = (MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop())- getPaddingBottom();
        int finalWidth = getFinalPatternViewSize(widthMeasureMode,suggestedWidth);
        int finalHeight = getFinalPatternViewSize(heightMeasureMode,suggestedHeight);

        int measuredSize = Math.min(finalWidth,finalHeight);
        setPatternViewDimension(measuredSize);
        measureAndSetNodes();
        setMeasuredDimension((measuredSize+getPaddingLeft())+getPaddingRight(),(measuredSize+getPaddingTop())+getPaddingBottom());

    }

    /**
     * Returns the final dimension for the PatternLockView from the size suggested in MeasureSpec.
     * Note: The size passed to this method doesn't calculate padding for the view. It should be explicitly handled.
     * @param measureMode The measuring mode from MeasureSpec
     * @param measureSize The suggested size from MeasureSpec
     * @return The final size without padding in int
     */

    private int getFinalPatternViewSize(int measureMode, int measureSize){
        int finalSize =0;
        switch(measureMode){
            case MeasureSpec.EXACTLY:
                finalSize = measureSize;
                break;
            case MeasureSpec.AT_MOST:
                finalSize = measureSize;
                break;
            case MeasureSpec.UNSPECIFIED:
                int unspecifiedSpacePixel= Math.round(DimensionConverter.convertDpToPixel(30f,getContext()));
                finalSize =  Math.round((getNodeRectSize()*3)+(unspecifiedSpacePixel*2));
                break;
        }
        return finalSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

            Paint rectPaint = getNodePaint();
            Paint linePaint = getLinePaint();

            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
                if(isDrawPattern()) {
                    canvas.drawLine(getPrevNodeX(), getPrevNodeY(), getCurrentMovementX(), getCurrentMovementY(), linePaint);
                    canvas.drawPath(getPatternPath(), linePaint);
                }
                for (Node node :getNodeList()){
                    if(!node.isNodeSelected()) {
                        rectPaint.setColor(node.nodeColor);
                        canvas.drawRoundRect(node.nodeRect, getNodeCornerSize(), getNodeCornerSize(), rectPaint);
                    }else
                    if (node.isNodeSelected() && !(isPatternError())){
                        rectPaint.setColor(node.nodeSelectedColor);
                        canvas.drawRoundRect(node.nodeSelectedRect,getNodeSelectedCornerSize(),getNodeSelectedCornerSize(),rectPaint);
                    }else if(isPatternError()){
                        rectPaint.setColor(Color.parseColor("#ef5350"));
                        linePaint.setColor(Color.parseColor("#ef5350"));
                        linePaint.setAlpha(125);
                        canvas.drawRoundRect(node.nodeSelectedRect,getNodeSelectedCornerSize(),getNodeSelectedCornerSize(),rectPaint);
                    }
                }
            }else{
                if(isDrawPattern()){
                canvas.drawLine(getPrevNodeX(),getPrevNodeY(),getCurrentMovementX(),getCurrentMovementY(),linePaint);
                canvas.drawPath(getPatternPath(),linePaint);
                }
                for (Node node :getNodeList()){
                    if(!node.isNodeSelected()) {
                        rectPaint.setColor(node.nodeColor);
                        canvas.drawRoundRect(node.nodeLeft, node.nodeTop, node.nodeRight, node.nodeBottom, getNodeCornerSize(), getNodeCornerSize(), rectPaint);
                    }else
                    if (node.isNodeSelected() && !isPatternError()){
                        rectPaint.setColor(node.nodeSelectedColor);
                        canvas.drawRoundRect(node.nodeSelectedLeft,node.nodeSelectedTop,node.nodeSelectedRight
                                ,node.nodeSelectedBottom,getNodeSelectedCornerSize(),getNodeSelectedCornerSize(),rectPaint);
                    }else if(isPatternError()) {
                        rectPaint.setColor(Color.parseColor("#ef5350"));
                        linePaint.setColor(Color.parseColor("#ef5350"));
                        linePaint.setAlpha(125);
                        canvas.drawRoundRect(node.nodeSelectedLeft,node.nodeSelectedTop,node.nodeSelectedRight
                                ,node.nodeSelectedBottom,getNodeSelectedCornerSize(),getNodeSelectedCornerSize(),rectPaint);
                    }
                }
            }

        }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if(!isPatternError() && getOnPatternChangedListener()!=null){
            return handlePatternGesture(event);
        }
        return false;
    }

    boolean handlePatternGesture(MotionEvent event){
        float pathX = event.getX();
        float pathY = event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                for(Node node:getNodeList()){
                    if(node.nodeTotalRect.contains(pathX,pathY)){
                        setPrevNodeX(node.nodeTotalRect.centerX());
                        setPrevNodeY(node.nodeTotalRect.centerY());
                        setCurrentMovementX(pathX);
                        setCurrentMovementY(pathY);
                        getPatternPath().moveTo(node.nodeTotalRect.centerX(),node.nodeTotalRect.centerY());
                        getOnPatternChangedListener().onPatternNodeSelected(node.nodeInt);
                        node.setNodeSelected(true);
                        setDrawPattern(true);
                        startNodeSelectedAnimation(node);
                        //Animate the node
                        return true;
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                for (Node node:getNodeList()){
                    if(node.nodeTotalRect.contains(pathX,pathY) && !(node.isNodeSelected())){
                        setPrevNodeX(node.nodeTotalRect.centerX());
                        setPrevNodeY(node.nodeTotalRect.centerY());
                        if(!isDrawPattern()){
                            getPatternPath().moveTo(node.nodeTotalRect.centerX(),node.nodeTotalRect.centerY());
                        }
                        getPatternPath().lineTo(node.nodeTotalRect.centerX(),node.nodeTotalRect.centerY());
                        getPatternPath().moveTo(node.nodeTotalRect.centerX(),node.nodeTotalRect.centerY());
                        getPathMeasure().setPath(getPatternPath(),false);
                        while (getPathMeasure().nextContour()) {
                            for (Node nodePath : getNodeList()) {
                                getPathMeasure().getPosTan(getPathMeasure().getLength() * 0.5f, pathCentreCoordinates, null);
                                if (nodePath.nodeTotalRect.contains(pathCentreCoordinates[0], pathCentreCoordinates[1])
                                        && !nodePath.isNodeSelected()) {
                                    getOnPatternChangedListener().onPatternNodeSelected(nodePath.nodeInt);
                                    nodePath.setNodeSelected(true);
                                    Log.d("AppLock", "Path in " + nodePath.nodeInt);
                                }
                            }
                        }
                        getOnPatternChangedListener().onPatternNodeSelected(node.nodeInt);
                        node.setNodeSelected(true);
                        setDrawPattern(true);
                        startNodeSelectedAnimation(node);
                        //Animate the node
                    }else if(isDrawPattern()){
                        setCurrentMovementX(pathX);
                        setCurrentMovementY(pathY);
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                setCurrentMovementX(getPrevNodeX());
                setCurrentMovementY(getPrevNodeY());
                getOnPatternChangedListener().onPatternCompleted(true);
                invalidate();
                return false;
        }

        return true;
    }
}
