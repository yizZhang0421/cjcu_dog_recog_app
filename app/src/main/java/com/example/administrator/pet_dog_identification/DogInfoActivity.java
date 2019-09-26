package com.example.administrator.pet_dog_identification;

import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class DogInfoActivity extends AppCompatActivity {
    public static String dog_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_info);

        ImageView image = findViewById(R.id.mImage);
        TextView name = findViewById(R.id.mDogName);
        TextView age = findViewById(R.id.mAge);
        TextView gender = findViewById(R.id.mGender);
        TextView personality = findViewById(R.id.mPersonality);
        switch (dog_info){
            case "Julian":
                image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.julian));
                name.setText(name.getText()+"朱利安");
                age.setText(age.getText()+"?");
                gender.setText(gender.getText()+"女");
                personality.setText(personality.getText()+"?");
                break;
            case "OldMi":
                image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.oldmi));
                name.setText(name.getText()+"老米");
                age.setText(age.getText()+"?");
                gender.setText(gender.getText()+"男");
                personality.setText(personality.getText()+"?");
                break;
            case "Tiger":
                image.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.tiger));
                name.setText(name.getText()+"虎皮");
                age.setText(age.getText()+"17");
                gender.setText(gender.getText()+"男");
                personality.setText(
                        personality.getText()+
                        "孩子氣的孤獨老人家\n"+
                        "吃飯睡覺的狀態下不喜歡被摸\n"+
                        "對人愛理不理 (禁地: 前腳)");
                break;
        }
    }
}
