package us.shiroyama.android.vulture.sample.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Sample {@link Parcelable} Class
 *
 * @author Fumihiko Shiroyama
 */

public class MyParcelable implements Parcelable {

    protected MyParcelable(Parcel in) {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {
        @Override
        public MyParcelable createFromParcel(Parcel in) {
            return new MyParcelable(in);
        }

        @Override
        public MyParcelable[] newArray(int size) {
            return new MyParcelable[size];
        }
    };
}
