package com.example.administrator.pet_dog_identification;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.HashMap;

public class ResponseActivity extends AppCompatActivity implements View.OnClickListener {
    public static boolean isReponseActivity;
    private HashMap<Button, String> button_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        byte[] bytes = MainActivity.bytes;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ImageView image = (ImageView) findViewById(R.id.mImageView);
        image.setImageBitmap(bmp);
        if(MainActivity.return_string.equals("nodog")){
            Toast.makeText(this, "未偵測到狗", Toast.LENGTH_SHORT).show();
        }
        else {
            String[] boxdata = MainActivity.return_string.split("\n");
            button_map = new HashMap<>();
            RelativeLayout layout = findViewById(R.id.mRelativeLayout);
            for (String box : boxdata) {
                String[] data = box.trim().split(" +");
                String label = data[0];
                int x = Integer.parseInt(data[1]);
                int y = Integer.parseInt(data[2]);
                int width = Integer.parseInt(data[3]) - Integer.parseInt(data[1]) + 1;
                int height = Integer.parseInt(data[4]) - Integer.parseInt(data[2]) + 1;
                int width_button = (int) (width * 0.3);

                Button button = new Button(this);
                button.setOnClickListener(this);
                button.setBackground(getDrawable(R.drawable.round_button));
                button.setX(x + (width / 2) - (width_button / 2));
                button.setY(y + (height / 2) - (width_button / 2));
                button.setLayoutParams(new ViewGroup.LayoutParams(width_button, width_button));
                layout.addView(button);
                button_map.put(button, label);
            }
        }
    }

    @Override
    public void onClick(View view) {
        DogInfoActivity.dog_info=button_map.get(view);
        ResponseActivity.this.startActivity(new Intent(ResponseActivity.this, DogInfoActivity.class));
    }
    @Override
    protected void onPause() {
        super.onPause();
        isReponseActivity=false;
    }
}
