package testbean;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

/**
 * Created by tianyang on 17/12/28.
 */
public class Player implements Serializable {

    public String name;
    public int age;

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    public Player() {
        name = UUID.randomUUID().toString();
        age = new Random().nextInt(100);
    }

    public Player(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
