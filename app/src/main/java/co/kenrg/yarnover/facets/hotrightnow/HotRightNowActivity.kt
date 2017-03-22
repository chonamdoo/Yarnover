package co.kenrg.yarnover.facets.hotrightnow

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.util.Pair
import android.view.View
import co.kenrg.yarnover.R
import co.kenrg.yarnover.api.ApiManager.api
import co.kenrg.yarnover.ext.setTaskDescription
import co.kenrg.yarnover.facets.hotrightnow.adapter.PatternDelegatorAdapter
import co.kenrg.yarnover.facets.hotrightnow.adapter.ViewItem
import co.kenrg.yarnover.facets.patterndetails.PatternDetailsActivity
import co.kenrg.yarnover.facets.patterndetails.PatternDetailsActivity.Companion.KEY_PATTERN_DATA
import co.kenrg.yarnover.iface.adapter.InfiniteScrollListener
import co.kenrg.yarnover.ui.drawer.BaseDrawerActivity
import kotlinx.android.synthetic.main.activity_hotrightnow.*
import kotlinx.android.synthetic.main.component_patterncard.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class HotRightNowActivity : BaseDrawerActivity() {
  companion object {
    private val KEY_PARCEL = "HOT_RIGHT_NOW_ACTIVITY_PARCEL"
  }

  private val activity: HotRightNowActivity = this
  private val ravelryApi = api()
  private val patternsAdapter = PatternDelegatorAdapter(onPatternClick = { item, view ->
    this.handleSelectPattern(item, view)
  })

  private var currentPage = 0

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    val patterns = patternsAdapter.getPatterns()
    if (patterns.isNotEmpty())
      outState?.putParcelable(KEY_PARCEL, HotRightNowParcel(currentPage, patterns))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_hotrightnow)
    setTaskDescription()
    setupDrawer(drawerLayout, toolbar, drawerNavigation)

    toolbar.apply {
      activity.setSupportActionBar(this)
      activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
      activity.supportActionBar?.setHomeButtonEnabled(true)
    }

    patternList.apply {
      this.adapter = patternsAdapter

      val linearLayoutManager = LinearLayoutManager(context)
      this.layoutManager = linearLayoutManager

      this.clearOnScrollListeners()
      this.addOnScrollListener(InfiniteScrollListener(linearLayoutManager) {
        requestPatterns(currentPage + 1)
      })
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PARCEL)) {
      val parcel = savedInstanceState.get(KEY_PARCEL) as HotRightNowParcel
      currentPage = parcel.currentPage
      patternsAdapter.replaceWithPatterns(parcel.patterns)
    } else {
      requestPatterns(currentPage + 1)
    }
  }

  fun requestPatterns(page: Int) {
    doAsync {
      val response = ravelryApi.searchPatterns(page = page).execute()

      uiThread {
        if (!response.isSuccessful) {
          Snackbar.make(container, "There was a problem fetching patterns", Snackbar.LENGTH_LONG)
              .setAction("Retry") { requestPatterns(page) }
              .setDuration(3000)
              .show()
        } else {
          currentPage = page
          val patternViewItems = response.body().patterns.map { (id, name, _, firstPhoto, designer) ->
            val photoUrl = firstPhoto.mediumUrl ?: firstPhoto.medium2Url ?: firstPhoto.squareUrl
            ViewItem.Pattern(id, name, designer.name, photoUrl)
          }
          patternsAdapter.addPatterns(patternViewItems)
        }
      }
    }
  }

  fun handleSelectPattern(pattern: ViewItem.Pattern, view: View) {
    val intent = Intent(this, PatternDetailsActivity::class.java)
    intent.putExtra(KEY_PATTERN_DATA, pattern)

    val bundle = getSharedElementTransitionBundle(view)
    if (bundle != null)
      startActivity(intent, bundle)
    else
      startActivity(intent)
  }

  private fun getSharedElementTransitionBundle(patternView: View): Bundle? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val animation = ActivityOptions.makeSceneTransitionAnimation(this, Pair.create(
          patternView.previewImage as View,
          getString(R.string.transition_preview_image)
      ))
      return animation.toBundle()
    } else {
      return null
    }
  }
}
