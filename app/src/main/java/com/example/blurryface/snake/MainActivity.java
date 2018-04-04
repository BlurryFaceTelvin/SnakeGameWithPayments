package com.example.blurryface.snake;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity {
    //canvas to draw
    Canvas canvas;
    //instance of the surface view
    SnakeAnimationView snakeAnimationView;
    //snake head animation
    Bitmap snakeHeadAnim;
    //drawing area
    Rect rectToBeDrawn;
    int frameHeight = 64,frameWidth =64,frameNumber,numFrames = 6;
    //variables for the screen width and height of the android phones
    int screenWidth,screenHeight;
    //variables to be displayed of the user current status
    public static int hiScore;
    int defaultHighScore=0,fps;
    long lastFrameTime;
    //variabes for the sharedpreferences where we store the high scores
    SharedPreferences preferences;
    Point size;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        //initialize the screen width and height
        screenHeight = size.y;
        screenWidth = size.x;
        snakeHeadAnim = BitmapFactory.decodeResource(getResources(),R.drawable.head_sprite_sheet);
        //initialise sharedpreferences
        preferences = getSharedPreferences("snakeScores",MODE_PRIVATE);
        hiScore = preferences.getInt("highscore",defaultHighScore);
        //initialise the view
        snakeAnimationView = new SnakeAnimationView(this);
        setContentView(snakeAnimationView);
        //set up the intent to the gameactivity
        intent = new Intent(this,GameActivity.class);
    }
    //handle when user presses back button
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            snakeAnimationView.pause();
            finish();
            return true;
        }
        return false;
    }
    @Override
    protected void onResume() {
        super.onResume();
        snakeAnimationView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeAnimationView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        while (true){
            snakeAnimationView.pause();
            break;
        }
        finish();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //once the user touches the screen we start the game
        startActivity(intent);
        return true;
    }

    //set up the view for the game
    public class SnakeAnimationView extends SurfaceView implements Runnable{
        SurfaceHolder ourHolder;
        Thread ourThread=null;
        // volatile ensures changes to the variable on all threads
        volatile boolean playingSnake;
        Paint paint;
        public SnakeAnimationView(Context context) {
            super(context);
            //initialise our variables
            ourHolder = getHolder();
            paint = new Paint();
            //getting one head that is animated
            frameWidth = snakeHeadAnim.getWidth()/numFrames;
            frameHeight = snakeHeadAnim.getHeight();
        }

        @Override
        public void run() {
            //when playing draw, control frames speed and update
            while (playingSnake){
                update();
                draw();
                controlFps();
            }
        }
        public void draw(){
            //check whether the view holder is valid
            //if not valid return
            if(!ourHolder.getSurface().isValid()){
                return;
            }
            canvas = ourHolder.lockCanvas();
            //if valid draw background
            canvas.drawColor(Color.BLACK);
            //set the colors of our text
            paint.setColor(Color.argb(255,255,255,255));
            paint.setTextSize(150);
            canvas.drawText("Snake Game",10,150,paint);
            paint.setTextSize(45);
            canvas.drawText("High Score: "+hiScore,10,screenHeight-50,paint);
            //have a rectangular area where we will have the snake head animation
            Rect desRect = new Rect(screenWidth/2-100,screenHeight/2-100,screenWidth/2+100,screenHeight/2+100);
            canvas.drawBitmap(snakeHeadAnim,rectToBeDrawn,desRect,paint);
            //unlock the canvas and post it
            ourHolder.unlockCanvasAndPost(canvas);
        }
        public void update(){
            //frame to be drawn
            rectToBeDrawn = new Rect((frameNumber*frameWidth)-1,0,(frameNumber*frameWidth+frameWidth)-1,frameHeight);
            frameNumber++;
            //ensure you draw frames that exist
            if(frameNumber==numFrames)
                frameNumber=0;
        }
        //ensures the animation appears at a sensible rate
        public void controlFps(){
            long timeThisFrame = System.currentTimeMillis() - lastFrameTime;
            long timeToSleep = 15 - timeThisFrame;
            if(timeThisFrame>0)
            {
                fps = (int) (100/timeThisFrame);
            }
            if(timeToSleep>0)
            {
                try {
                    Thread.sleep(timeToSleep);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }
        public void pause(){
            playingSnake=false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        public void resume(){
            playingSnake=true;
            ourThread = new Thread(this);
            ourThread.start();
        }

    }
}
