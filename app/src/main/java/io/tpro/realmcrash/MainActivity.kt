package io.tpro.realmcrash

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import kotlinx.coroutines.*

open class Foo: RealmObject() {
    @PrimaryKey var id: Int = 0
    var bar: Bar? = null
}

open class Bar(idd: Int): RealmObject() {
    @PrimaryKey var id: Int = idd
    constructor(): this(0)
}

/**
 * This test is exaggerated, the crash on the app happened with a very low number of calls,
 * but in multiple locations and different threads.
 */
class MainActivity: AppCompatActivity() {

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    var foo: Foo? = null
    lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Realm.init(this)
        realm = Realm.getDefaultInstance()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        //This is insane
        megaton()
        megaton()
        megaton() //If you remove this one, the crash stops
//        megaton() //Uncomment these above if the crash does not happen.
//        megaton()
//        megaton()
//        megaton()
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
        realm.close()
    }

    //Called Atom by the Church of the Children of Atom and also the Apostles of the Holy Light
    private fun megaton() {

        foo = realm.where(Foo::class.java).findAll().firstOrNull()
        foo?.removeAllChangeListeners()
        foo?.addChangeListener<Foo> { _, _ ->
            foo?.removeAllChangeListeners()
            foo?.addChangeListener<Foo> { _, _ -> }
        }

        uiScope.launch(Dispatchers.Main) {

            val d = this.async(Dispatchers.IO) {
                val realmm = Realm.getDefaultInstance()
                realmm.executeTransaction {
                    realmm.where(Foo::class.java).findAll().firstOrNull()?.let {
                        //Abuse of power
                        it.bar = realmm.copyToRealmOrUpdate(Bar(0))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(1))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(2))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(3))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(4))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(5))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(6))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(7))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(8))
                        it.bar = realmm.copyToRealmOrUpdate(Bar(9))
                    } ?: {
                        val newFoo = Foo()
                        newFoo.bar = Bar(0)
                        realmm.insertOrUpdate(newFoo)
                    }()
                }
                realmm.close()
            }

            d.await()

            megaton()

            //This is overkill
            for (i in 0 until 100) {
                foo?.removeAllChangeListeners()
                foo?.addChangeListener<Foo> { _, _ -> }
            }

        }

    }

}
