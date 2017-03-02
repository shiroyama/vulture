package us.shiroyama.android.vulture.sample.serializers;

import android.os.Parcelable;

import org.parceler.Parcels;

import java.util.List;

import us.shiroyama.android.vulture.sample.data.User;
import us.shiroyama.android.vulture.serializers.CustomSerializer;

/**
 * @author Fumihiko Shiroyama
 */

public class ParcelSerializer implements CustomSerializer<List<User>, Parcelable> {
    @Override
    public Parcelable serialize(List<User> users) {
        return Parcels.wrap(users);
    }

    @Override
    public List<User> deserialize(Parcelable parcelable) {
        return Parcels.unwrap(parcelable);
    }
}
