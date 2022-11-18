/*
 * MIT License
 *
 * Copyright (c) 2020 Shreyas Patil
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.shreyaspatil.foodium.ui.details

import GlideImageGetter
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.text.style.QuoteSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import dev.shreyaspatil.foodium.R
import dev.shreyaspatil.foodium.databinding.ActivityPostDetailsBinding
import dev.shreyaspatil.foodium.ui.base.BaseActivity
import dev.shreyaspatil.foodium.utils.QuoteSpanClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject


private const val TAG="PostDetailsActivity"
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class PostDetailsActivity : BaseActivity<PostDetailsViewModel, ActivityPostDetailsBinding>() {

    @Inject
    lateinit var viewModelFactory: PostDetailsViewModel.PostDetailsViewModelFactory

    override val mViewModel: PostDetailsViewModel by viewModels {
        val postId = intent.extras?.getInt(KEY_POST_ID)
            ?: throw IllegalArgumentException("`postId` must be non-null")

        PostDetailsViewModel.provideFactory(viewModelFactory, postId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mViewBinding.root)

        setSupportActionBar(mViewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        initPost()

    }

    private fun initPost() {
        mViewModel.post.observe(this) { post ->
            mViewBinding.postContent.apply {
                postTitle.text = post.title
                postAuthor.text = post.author
                //富文本图片处理
                val imageGetter = GlideImageGetter(resources, postBody)
                val styledText = HtmlCompat.fromHtml(
                    post.body.toString(),
                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                    imageGetter,
                    null
                )
                ImageClick(styledText as Spannable)
                replaceQuoteSpans(styledText )
                // 超链接预览
                postBody.movementMethod = LinkMovementMethod.getInstance()
                postBody.text =    styledText

            }
            mViewBinding.imageView.load(post.imageUrl)
        }
    }
    //文本图片样式设置
    private fun replaceQuoteSpans(spannable: Spannable) {

        val quoteSpans: Array<QuoteSpan> =
            spannable.getSpans(0, spannable.length - 1, QuoteSpan::class.java)

        for (quoteSpan in quoteSpans) {
            val start: Int = spannable.getSpanStart(quoteSpan)
            val end: Int = spannable.getSpanEnd(quoteSpan)
            val flags: Int = spannable.getSpanFlags(quoteSpan)
            spannable.removeSpan(quoteSpan)
            spannable.setSpan(
                QuoteSpanClass(
                    ContextCompat.getColor(this, R.color.Grey),
                    ContextCompat.getColor(this, R.color.colorAccent),
                    10F,
                    50F
                ),
                start,
                end,
                flags
            )
        }

    }

    //图片点击
    fun ImageClick(html: Spannable) {
        for (span in html.getSpans(0, html.length, ImageSpan::class.java)) {
            val flags = html.getSpanFlags(span)
            val start = html.getSpanStart(span)
            val end = html.getSpanEnd(span)
            html.setSpan(object : URLSpan(span.source) {
                override fun onClick(v: View) {
                    Log.d(TAG, "onClick: url is ${span.source}")

                    loadPhoto("${span.source}")

                }
            }, start, end, flags)
        }
    }
    //图像弹窗
    private fun loadPhoto(filepath: String) {
        Log.d("FAILS", "1")
        val dialog:Dialog = Dialog(this);

        val inflater = this.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val layout: View = inflater.inflate(
            R.layout.photo_view_dialog,
            null
        )
        val image: PhotoView = layout.findViewById<View>(R.id.photo_view) as PhotoView

        Picasso.get().load("${filepath}").into(image);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(layout);
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        dialog.window?.setLayout( width, (2 * height)/5);
        dialog.setCanceledOnTouchOutside(true);
        dialog.create()
        dialog.show()
    }


    private fun share() {
        val post = mViewModel.post.value ?: return
        val shareMsg = getString(R.string.share_message, post.title, post.author)

        val intent = ShareCompat.IntentBuilder.from(this)
            .setType("text/plain")
            .setText(shareMsg)
            .intent

        startActivity(Intent.createChooser(intent, null))
    }

    override fun getViewBinding(): ActivityPostDetailsBinding =
        ActivityPostDetailsBinding.inflate(layoutInflater)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                return true
            }

            R.id.action_share -> {
                share()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_POST_ID = "postId"

        fun getStartIntent(
            context: Context,
            postId: Int
        ) = Intent(context, PostDetailsActivity::class.java).apply { putExtra(KEY_POST_ID, postId) }
    }
}
