/* Requires:

 */

package com.example.jarvisv11;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by howardzhang on 11/7/17.
 */

public class Firebase {
    private DatabaseReference database;
    public List<Task> tasks = new ArrayList<Task>();
    public List<Person> persons = new ArrayList<Person>();
    public final MainActivity GUI;

    public Firebase(MainActivity GUI){
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        this.GUI = GUI;
        database = FirebaseDatabase.getInstance().getReference();

        //ALL DATABASE LISTENERS
        database.child("persons").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                persons = new ArrayList<Person>();
                for(DataSnapshot personDataSnapshot : dataSnapshot.getChildren()){
                    Person person = personDataSnapshot.getValue(Person.class);
                    persons.add(person);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Firebase.this.GUI.displayToast(databaseError.getMessage());
            }
        });
        database.child("tasks").addValueEventListener(new ValueEventListener(){

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tasks = new ArrayList<Task>();
                for(DataSnapshot taskDataSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskDataSnapshot.getValue(Task.class);
                    tasks.add(task);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Firebase.this.GUI.displayToast(databaseError.getMessage());
            }
        });
        database.child("log").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot logDataSnapshot : dataSnapshot.getChildren()){
                    Log log = logDataSnapshot.getValue(Log.class);
                    Firebase.this.GUI.cancelSphinxRec();
                    Firebase.this.GUI.cardSwipe(log);
                }
                database.child("log").removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Firebase.this.GUI.displayToast(databaseError.getMessage());
            }
        });
    }

    //Run the motor
    public void motor(){
        database.child("motor").setValue(1);
    }

    //Changing Here/Away
    public void here_away(String here_away, String name){
        database.child("where").child(name).setValue(here_away);
    }

    //Adding a Person
    public void addPerson(Person person){
        person.setUID(database.child("persons").push().getKey());
        database.child("persons").child(person.getUID()).setValue(person);
    }
    //Updating a Person level
    public void updatePersonLevel(Person person, int level){
        database.child("persons").child(person.getUID()).child("level").setValue(level);
    }

    //Tasks: Adding, Removing
    public void addTask(String name, String desc, String due){
        Task task = new Task();
        task.setUID(database.child("tasks").push().getKey());
        task.setName(name);
        task.setDesc(desc);
        task.setDue(due);
        task.setUser(GUI.user.getName().toLowerCase());
        database.child("tasks").child(task.getUID()).setValue(task);
    }

    public void removeTask(int index){
        database.child("tasks").child(tasks.get(index).getUID()).removeValue();
    }
}
