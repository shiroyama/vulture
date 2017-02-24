# Vulture

Vulture is an Android library that let you handle asynchronous callbacks properly within Activity/Fragment's life cycle.

Vulture frees you from the hassle with `IllegalStateException` when coping with `FragmentTransaction#commit()` after onSaveInstanceState.

## How to Use

Say you have `fetchAsynchronously()` method to feth a message from a server asynchronously then call `doCallback(message)` to show DialogFragment like below,

```java
void fetchAsynchronously() {
    /* do heavy asynchronous task here */
    doCallback("finished!");
}

void doCallback(@NonNull String message) {
    FinishDialog.newInstance().show(getSupportFragmentManager(), "TAG");
}
```

You simply annotate your Activity with `@ObserveLifecycle`, then annotate your methods with `@SafeCallback` that have to be callbacked safely like below.

Vulture automatically generates a class named `SafeMainActivity` that has corresponding methods with `@SafeCallback` annotations at compile time without any Reflection APIs.

```java
@ObserveLifecycle
public class YourActivity extends AppCompatActivity {
    void fetchAsynchronously() {
        SafeMainActivity.doCallbackSafely("finished!");
    }

    @SafeCallback
    void doCallback(@NonNull String message) {
        FinishDialog.newInstance().show(getSupportFragmentManager(), "TAG");
    }
}
```

When you call `SafeMainActivity.doCallbackSafely(message)` after asynchronous task, the actual `doCallback(message)` is called only within crash free window.

Finally, call `register()/unregister()` methods to let Vulture know about the safe window.

```java
@Override
protected void onResume() {
    super.onResume();
    SafeMainActivity.register(this);
}

@Override
protected void onPause() {
    SafeMainActivity.unregister();
    super.onPause();
}
```

Check `sample` and generated codes for details.

## Installation

[ ![Download](https://api.bintray.com/packages/srym/maven/vulture/images/download.svg) ](https://bintray.com/srym/maven/vulture/_latestVersion)

```
annotationProcessor 'us.shiroyama.android:vulture-processor:0.2.1'
compile 'us.shiroyama.android:vulture:0.2.1'
```

## Limitations

Vulture is now alpha release and there are a few limitations.

 * Supported types for method arguments annotated with `@SafeCallback` are `primitive types` (including its boxed types), `String`, `Bundle`, `Parcelable` so far.
 * Lint for processor is not implemented properly yet.

## Under the Hood

Vulture is based on the brilliant idea of `PauseHandler` discussed at [Stack Overflow](http://stackoverflow.com/questions/8040280/how-to-handle-handler-messages-when-activity-fragment-is-paused "How to handle Handler messages when activity/fragment is paused"). Thank you.

## License

```
Copyright 2017 Fumihiko Shiroyama

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
