package dev.mooner.peekalert.demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import dev.mooner.peekalert.PeekAlert;

public class JavaExample extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        PeekAlert peekAlert = PeekAlert.create(this)
                .setDraggable(true)
                .setTitle("Hello, PeekAlert!")
                .setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");

        peekAlert.peek();
    }
}
