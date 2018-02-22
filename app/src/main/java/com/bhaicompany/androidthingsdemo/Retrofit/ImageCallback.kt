package  com.bhaicompany.androidthingsdemo.Retrofit

import org.json.JSONArray

/**
 * Created by ashwin-4529 on 18/02/18.
 */

interface ImageCallback{
    fun updateImage(jsonArray: JSONArray)
}