# nucleus
[![API](https://img.shields.io/badge/API-17%2B-%234CC93C.svg)](https://android-arsenal.com/api?level=17)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![API](https://img.shields.io/badge/Made%20with-Kotlin-blue.svg)
![API](https://img.shields.io/badge/State-In%20progress-orange.svg)


Nucleus is a Odoo Json RPC client for Android. This RPC let you to adapt some Odoo module to Android in a simple way.

<p style="text-align: center;">

<img src="https://raw.githubusercontent.com/miguel-domatix/nucleus/master/android/doc/sales.png"  alt="sale image example" width="250"/>
&emsp;
<img src="https://raw.githubusercontent.com/miguel-domatix/nucleus/master/android/doc/contactos-1.png" alt="contacts image example" width="250"/>
</p>

## How to use

Odoo specific methods can be access using [singleton objects](https://kotlinlang.org/docs/reference/object-declarations.html#object-declarations) `Odoo`.
Login Account related fuctionally can be access using `Context`'s [Extension functions](https://kotlinlang.org/docs/reference/extensions.html#extension-functions).
`Authentification` as well as `Sessions` are managed inside application's `core` module. You should not use any `session` related methods anywhere, it may lead to unexpected behaviour.

# Examples

## Create

Creates a new record for the model

**Request**
```kotlin
Odoo.create(model = "res.partner", values = mapOf(
        "name" to "example", "email" to "example@example.com"
))  {
    onSubscribe {
      // Disposable
    }
    onNext {
      // Response
    }
    onError {
      // Deal the error
    }
    onComplete {
      // Your code
    }
  }
```

**Result**
```Json
{
  "result": 45
}
```

## Read

Reads the requested fields for the records

**Request**
```kotlin
Odoo.read(model = "res.partner", ids = listOf(1, 3), fields = listOf("id", "name", "email"))
    {
    onSubscribe {
      // Disposable
    }
    onNext {
      // Response
    }
    onError {
      // Deal the error
    }
    onComplete {
      // Your code
    }
  }
  ```
  **Result**
  ```Json
  {
    "result": [
      {
        "email": "info@yourcompany.example.com",
        "id": 1,
        "name": "YourCompany"
      },
      {
        "email": "admin@yourcompany.example.com",
        "id": 3,
        "name": "Administrator",
      }
    ]
  }
```
<p style="text-align: center;">
<img src="https://raw.githubusercontent.com/miguel-domatix/nucleus/master/android/doc/Peek%2028-05-2019%2012-34.gif" alt="gif refresh example" width="250"/>
&emsp;
<img src="https://raw.githubusercontent.com/miguel-domatix/nucleus/master/android/doc/Peek%2028-05-2019%2012-36.gif" alt="activity done example" width="250"/>
</p>

## Used libraries

- RxJava
- Retrofit
- OkHttp
- Glide
- Timber
- Data Binding
- Gson

## License

This project is licensed under the [MIT License](http://opensource.org/licenses/MIT).
