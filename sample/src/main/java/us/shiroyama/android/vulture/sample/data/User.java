package us.shiroyama.android.vulture.sample.data;

import org.parceler.Parcel;

/**
 * @author Fumihiko Shiroyama
 */

@Parcel
public class User {
    String name;
    int age;

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
