package tech.nagual.phoenix.tools.organizer.di

import android.content.Context
import android.text.util.Linkify
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.FragmentScoped
import io.noties.markwon.*
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import tech.nagual.common.R
import tech.nagual.common.extensions.resolveAttribute
import tech.nagual.phoenix.tools.organizer.editor.markdown.*
import tech.nagual.phoenix.tools.organizer.utils.coil.CoilImagesPlugin

@Module
@InstallIn(FragmentComponent::class)
object MarkwonModule {

    @Provides
    @FragmentScoped
    fun provideMarkwon(@ActivityContext context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(LinkifyPlugin.create(Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS))
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(MovementMethodPlugin.create(BetterLinkMovementMethod.getInstance()))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver(LinkResolverDef())
                }
            })
            .usePlugin(CoilImagesPlugin.create(context))
            .apply {
                val mainColor = context.resolveAttribute(R.attr.colorMarkdownTask) ?: return@apply
                val backgroundColor =
                    context.resolveAttribute(R.attr.colorBackground) ?: return@apply
                usePlugin(TaskListPlugin.create(mainColor, mainColor, backgroundColor))
            }
            .build()
    }

    @Provides
    @FragmentScoped
    fun provideMarkwonEditor(markwon: Markwon): MarkwonEditor {
        return MarkwonEditor.builder(markwon)
            .useEditHandler(EmphasisEditHandler())
            .useEditHandler(StrongEmphasisEditHandler())
            .useEditHandler(CodeHandler())
            .useEditHandler(CodeBlockHandler())
            .useEditHandler(BlockQuoteHandler())
            .useEditHandler(StrikethroughHandler())
            .useEditHandler(HeadingHandler())
            .build()
    }
}
