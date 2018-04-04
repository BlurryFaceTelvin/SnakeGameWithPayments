package com.example.blurryface.snake;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.africastalking.AfricasTalking;
import com.africastalking.models.payment.B2CResponse;
import com.africastalking.models.payment.Business;
import com.africastalking.models.payment.Consumer;
import com.africastalking.models.payment.checkout.CheckoutResponse;
import com.africastalking.models.payment.checkout.MobileCheckoutRequest;
import com.africastalking.services.PaymentService;
import com.africastalking.utils.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dmax.dialog.SpotsDialog;

import static com.example.blurryface.snake.MainActivity.hiScore;


/**
 * Created by BlurryFace on 3/13/2018.
 */

public class GameActivity extends Activity {
    //canvas to draw
    Canvas canvas;
    //instance of the surface view
    SnakeView snakeView;
    //snakes head,body and tail
    Bitmap headBitmap,bodyBitmap,tailBitmap;
    //apple
    Bitmap appleBitmap;
    //variables for the sound
    private SoundPool soundPool;
    int sample1=-1,sample2=-1,sample3=-1,sample4=-1;
    //direction 0=up,1=right,2=down,3=left
    int directionOfTravel = 0;
    //variables for the screen width and height of the android phones
    int screenWidth,screenHeight,topGap;
    //variable to keep track of player scores
    int scores = 0;
    //stats
    int fps;
    long lastFrameTime;
    //coordinates of the snake x and y
    int[] snakeX,snakeY;
    int appleX,appleY;
    //keeps track of the snake length
    int snakeLength;
    //size of the pixel on the game board
    int blockSize,numBlockWide,numBlockHigh;
    //variabes for the sharedpreferences where we store the high scores
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Point size;
    Display display;
    //snake speed
    int speed = 100;
    Request request;
    OkHttpClient client;
    SpotsDialog dialog,payDialog;
    int status;
    boolean onFirstResume;
    AlertDialog alertDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //initialise sounds
        soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        sample1 = soundPool.load(this,R.raw.sample1,0);
        sample2 = soundPool.load(this,R.raw.sample2,0);
        sample3 = soundPool.load(this,R.raw.sample3,0);
        sample4 = soundPool.load(this,R.raw.sample4,0);
        //configure the display
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        screenHeight= size.y;
        screenWidth = size.x;
        topGap = screenHeight/14;
        blockSize = screenWidth/40;
        numBlockWide = 40;
        numBlockHigh = (screenHeight-topGap)/blockSize;
        //initialise the head,body,tail and apple
        headBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.purpledot);
        tailBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.whitedot);
        bodyBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.reddot);
        appleBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.apple);
        //scale our bitmaps
        headBitmap = Bitmap.createScaledBitmap(headBitmap,blockSize,blockSize,false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap,blockSize,blockSize,false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap,blockSize,blockSize,false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap,blockSize,blockSize,false);
        //initialise sharedpreferences
        preferences = getSharedPreferences("snakeScores",MODE_PRIVATE);
        //initialise progress dialog
        payDialog = new SpotsDialog(this,"LOADING");
        dialog = new SpotsDialog(this,"Processing");
        //AfricasTalking
        try {
            AfricasTalking.initialize("192.168.1.196",35897,true);
        }catch (Exception e){
            e.printStackTrace();
        }
        //set our status to 0 to mean first resume
        onFirstResume = true;
        status = 0;
        //initialise our view
        snakeView = new SnakeView(this);
        setContentView(snakeView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
        if(status==5){
            //pause by the checkout
            status = 5;
            Log.e("pause",String.valueOf(status));
        }
        else {
            //normal pause
            status=3;
            Log.e("pause",String.valueOf(status));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(onFirstResume){
            onFirstResume = false;
            Log.e("resume",String.valueOf(status));
            snakeView.resume();
        }else if(!onFirstResume&&status==5) {
            //after mpesa pop up
            status = 3;
            Log.e("resume",String.valueOf(status));
            dialog.show();
            //wait for ten seconds to confirm
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    confirmPayment();
                }
            }, 10000);

        }else{
            snakeView.resume();
            Log.e("resume","normal");
        }

    }
    /*
    @Override
    protected void onStop() {
        super.onStop();
        while (true){
            snakeView.pause();
            break;
        }
        finish();
    }
    */
    //handle when user presses back button
    public boolean onKeyDown(int e, KeyEvent event){
        if(e==KeyEvent.KEYCODE_BACK){
            snakeView.pause();
            Intent i = new Intent(this,MainActivity.class);
            startActivity(i);
            finish();
            return  true;
        }
        return false;
    }

    //setting up our view
    public class SnakeView extends SurfaceView implements Runnable{
        SurfaceHolder ourHolder;
        Thread ourThread=null;
        // volatile ensures changes to the variable on all threads
        volatile boolean playingSnake;
        Paint paint;
        public SnakeView(Context context) {
            super(context);
            //initialise our variables
            ourHolder = getHolder();
            paint = new Paint();
            //snake length
            snakeX = new int[200];
            snakeY = new int[200];
            //initial snake length
            snakeLength=3;
            //starting snake
            getSnake();
            //stating apple
            getApple();

        }
        public void getSnake(){

            Log.e("snakeLength",String.valueOf(snakeLength));
            //start the snake head in the middle of the screen
            snakeX[0] = numBlockWide/2;
            snakeY[0] = numBlockHigh/2;
            //after the head place the body imediately behind the head in the same y position
            snakeX[1] = snakeX[0]-1;
            snakeY[1] = snakeY[0];
            //after the body place the tail imediately behind the body in the same y position
            snakeX[2] = snakeX[1]-1;
            snakeY[2] = snakeY[1];
        }
        public void getApple(){
            //put the apple in random positions and ensure the apple doesnt appear offscreen
            Random random = new Random();
            Log.e("block",String.valueOf(numBlockWide));
            appleX = random.nextInt((numBlockWide-1))+1;
            appleY = random.nextInt((numBlockHigh-1))+1;
        }

        @Override
        public void run() {
            while (playingSnake){
                update();
                draw();
                controlFps();
            }

        }
        //drawing our game objects
        public void draw(){
            //check if the canvas is valid
            //if not valid return
            if(!ourHolder.getSurface().isValid()){
                return;
            }
            canvas = ourHolder.lockCanvas();
            //set the background
            canvas.drawColor(Color.BLACK);
            paint.setColor(Color.argb(255,255,255,255));
            paint.setTextSize(topGap/2);
            canvas.drawText("Score: "+scores,10,topGap-6,paint);
            //draw the border lines
            paint.setStrokeWidth(3);
            canvas.drawLine(1,topGap,screenWidth-1,topGap,paint);
            canvas.drawLine(screenWidth-1,topGap,screenWidth-1,topGap+(numBlockHigh*blockSize),paint);
            canvas.drawLine(screenWidth-1,topGap+(numBlockHigh*blockSize),1,topGap+(numBlockHigh*blockSize),paint);
            canvas.drawLine(1,topGap+(numBlockHigh*blockSize),1,topGap,paint);
            //draw the snake
            canvas.drawBitmap(headBitmap,snakeX[0]*blockSize,(snakeY[0]*blockSize)+topGap,paint);
            //draw the body
            for (int i=1;i<snakeLength-1;i++){
                canvas.drawBitmap(bodyBitmap,snakeX[i]*blockSize,(snakeY[i]*blockSize)+topGap,paint);
            }
            //draw the tail
            canvas.drawBitmap(tailBitmap,snakeX[snakeLength-1]*blockSize,(snakeY[snakeLength-1]*blockSize)+topGap,paint);
            //draw the apple
            canvas.drawBitmap(appleBitmap,appleX*blockSize,(appleY*blockSize)+topGap,paint);
            ourHolder.unlockCanvasAndPost(canvas);

        }
        public void update(){
            //check if the player has eaten the apple
            if(snakeX[0]==appleX&&snakeY[0]==appleY) {
                //increase the length
                snakeLength++;
                //increase the score
                scores++;
                //place an apple in a different position
                getApple();
                //play a sound
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }
                //move the snake from the back
                for (int i=snakeLength;i>0;i--){
                    //move x position
                    snakeX[i] = snakeX[i-1];
                    //move y position
                    snakeY[i] = snakeY[i-1];
                }
                //move the head in appropriate position
                switch (directionOfTravel){
                    //up
                    case 0:
                        snakeY[0]--;
                        break;
                    //right
                    case 1:
                        snakeX[0]++;
                        break;
                    //down
                    case 2:
                        snakeY[0]++;
                        break;
                    //left
                    case 3:
                        snakeX[0]--;
                        break;
                }
                //variable to check if player is dead
                boolean dead = false;
                //player hits wall they are dead
                if(snakeX[0]<0)
                    dead=true;
                if (snakeX[0]>=numBlockWide)
                    dead=true;
                if(snakeY[0]<0)
                    dead=true;
                if(snakeY[0]>=numBlockHigh)
                    dead=true;
                //player eats themselves they are dead
                for (int i=snakeLength-1;i>0;i--){
                    //make sure snake is big enough to eat themselves
                    if((i>4)&&(snakeX[0]==snakeX[i])&&(snakeY[0]==snakeY[i])){
                        dead=true;
                    }
                }
                if(dead){
                    //check if score is higher than the high score
                    if(scores>hiScore){
                        hiScore = scores;
                        editor = preferences.edit();
                        editor.putInt("highscore",hiScore);
                        editor.apply();
                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //have an alert dialog for the payment
                            playingSnake = false;
                            final AlertDialog.Builder paymentalertDialog = new AlertDialog.Builder(GameActivity.this);
                            paymentalertDialog.setMessage("Would you like to pay for another chance?");
                            paymentalertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
//                                    resume();
//                                    getSnake();
//                                    Log.e("shhiii","Yes pressed");
                                     alertDialog.dismiss();
                                      payDialog.show();
                                      //payment class
                                      new Paying().execute();
                                      status = 5;
                                }
                            });
                            paymentalertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    alertDialog.dismiss();
                                    //go to the game over page
                                    Intent intent = new Intent(GameActivity.this,GameOverActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("score",String.valueOf(scores));
                                    startActivity(intent);
                                }
                            });
                            Log.e("whicj","asdsadsdc");
                            alertDialog = paymentalertDialog.create();
                            alertDialog.setCanceledOnTouchOutside(false);
                            alertDialog.show();
                        }
                    });


                }


        }
        public void controlFps(){
            long timeThisFrame = System.currentTimeMillis() - lastFrameTime;
            long timeToSleep = 100 - timeThisFrame;
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
        @Override
        public boolean onTouchEvent (MotionEvent event) {
            switch(event.getAction())
            {
                case MotionEvent.ACTION_UP:
                    if (event.getX()>=screenWidth/2) {
                        //turn right
                        directionOfTravel++;
                        //if no such direction go back to 0
                        if (directionOfTravel==4)
                            directionOfTravel=0;
                    }

                    else{
                        //turn left
                        directionOfTravel--;
                        //if no such direction
                        if(directionOfTravel<0)
                            directionOfTravel=3;
                    }
                    break;

            }
            return true;
        }
    }
    public void confirmPayment(){
        client = new OkHttpClient();
        request = new Request.Builder().url("http://192.168.137.80:30001/transaction/status").build();
        client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                dialog.dismiss();
                Log.e("failure",e.getMessage());
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                dialog.dismiss();
                String status = response.body().string();
                //if user either cancels or has insufficient funds we go to game over
                if(status.equals("Success")){
                    //if it fails to pay sends you to game over page
                    showMessage("failed");
                    //go to the game over page
                    Intent intent = new Intent(GameActivity.this,GameOverActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("score",String.valueOf(scores));
                    startActivity(intent);

                }else if(status.equals("Failed")){
                    dialog.dismiss();
                    //if successful add the time and player gets another chance to continue
                    showMessage("successful");
                    //if statement and if the payment is successful payment is done
                    snakeView.resume();
                    snakeView.getSnake();
                }

            }
        });
    }
    public void showMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GameActivity.this,"Your payment has "+message,Toast.LENGTH_LONG).show();
            }
        });
    }
    public class Paying extends AsyncTask<Void,String,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                final PaymentService paymentService = AfricasTalking.getPaymentService();
                MobileCheckoutRequest checkoutRequest = new MobileCheckoutRequest("MusicApp","KES 20","0703280748");
                paymentService.checkout(checkoutRequest, new Callback<CheckoutResponse>() {
                    @Override
                    public void onSuccess(CheckoutResponse data) {
                        payDialog.dismiss();
                        Toast.makeText(GameActivity.this,data.status,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        payDialog.dismiss();
                        Toast.makeText(GameActivity.this,throwable.getMessage(),Toast.LENGTH_LONG).show();
                        //go to the game over page
                        Intent intent = new Intent(GameActivity.this,GameOverActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("score",String.valueOf(scores));
                        startActivity(intent);

                    }
                });
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

