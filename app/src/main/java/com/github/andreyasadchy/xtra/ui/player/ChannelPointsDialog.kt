package com.github.andreyasadchy.xtra.ui.player

import com.github.andreyasadchy.xtra.R

import android.content.Intent
import androidx.core.net.toUri
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.model.ui.ViewerPoints
import com.github.andreyasadchy.xtra.model.ui.ViewerReward
import com.github.andreyasadchy.xtra.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/** Viewer actions always use the active account; mutations are never automatically retried. */
object ChannelPointsDialog {
    fun show(fragment: Fragment, channelId: String, login: String) {
        val context = fragment.requireContext()
        val account = context.tokenPrefs().getString(C.USER_ID, null)
        val headers = TwitchApiHelper.getGQLHeaders(context, true)
        fun sameAccount() = account != null && context.tokenPrefs().getString(C.USER_ID, null) == account
        fun openTwitch() {
            if (login.matches(Regex("[A-Za-z0-9_]+"))) fragment.startActivity(Intent(Intent.ACTION_VIEW, "https://www.twitch.tv/$login".toUri()))
        }
        if (account.isNullOrBlank() || headers[C.HEADER_TOKEN].isNullOrBlank()) {
            AlertDialog.Builder(context).setMessage(context.getString(R.string.hex_points_sign_in)).setPositiveButton(context.getString(R.string.hex_ok), null).show()
            return
        }
        val repository = (context.applicationContext as XtraApp).xtraModule.graphQLRepository
        val network = context.prefs().getString(C.NETWORK_LIBRARY, C.OKHTTP)
        val owner = fragment.viewLifecycleOwner
        val actions = PointsActionGate(requireNotNull(account))
        val actionButtons = mutableListOf<Pair<Button, Boolean>>()
        var loading: Job? = null
        var confirmation: AlertDialog? = null
        val density = context.resources.displayMetrics.density
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (16 * density).toInt(), (24 * density).toInt(), (16 * density).toInt())
        }
        val scroll = android.widget.ScrollView(context).apply { addView(content) }
        val dialog = AlertDialog.Builder(context).setTitle(context.getString(R.string.hex_channel_points)).setView(scroll)
            .setNegativeButton(context.getString(R.string.hex_close), null).setNeutralButton(context.getString(R.string.hex_open_twitch)) { _, _ -> openTwitch() }.create()
        dialog.show()
        fun updateActions() { actionButtons.forEach { (button, available) -> button.isEnabled = available && !actions.isBusy } }
        fun message(text: String) {
            actionButtons.clear()
            content.removeAllViews()
            content.addView(TextView(context).apply { this.text = text })
        }
        fun failure() { message("Twitch could not confirm this action. Check Twitch before trying again. You can use Open Twitch below.") }
        fun refresh() {
            loading?.cancel()
            message(context.getString(R.string.loading))
            loading = owner.lifecycleScope.launch {
                try {
                    val points = ViewerPoints.parse(repository.loadViewerPoints(network, headers, login))
                    if (!dialog.isShowing || !sameAccount()) return@launch
                    content.removeAllViews()
                    content.addView(TextView(context).apply { text = context.getString(R.string.hex_points_balance, points.balance?.let { java.text.NumberFormat.getIntegerInstance().format(it) } ?: context.getString(R.string.hex_unavailable)) })
                    content.addView(Button(context).apply {
                        text = context.getString(if (points.claimId == null) R.string.hex_no_bonus else R.string.hex_claim_bonus)
                        val available = points.claimId != null
                        isEnabled = available && !actions.isBusy
                        actionButtons += this to available
                        setOnClickListener {
                            if (!actions.claimBonus(context.tokenPrefs().getString(C.USER_ID, null))) return@setOnClickListener
                            updateActions()
                            owner.lifecycleScope.launch {
                                try {
                                    check(sameAccount())
                                    val result = repository.loadClaimPoints(network, headers, channelId, points.claimId)
                                    check(result.errors.isNullOrEmpty())
                                    if (sameAccount() && dialog.isShowing) refresh()
                                } catch (e: CancellationException) { throw e } catch (_: Exception) { if (dialog.isShowing) failure() }
                                finally { actions.finish(); updateActions() }
                            }
                        }
                    })
                    if (points.rewards.isEmpty()) content.addView(TextView(context).apply { text = context.getString(R.string.hex_no_custom_rewards) })
                    points.rewards.forEach { reward ->
                        content.addView(Button(context).apply {
                            text = context.getString(R.string.hex_reward_cost, reward.title, reward.cost)
                            val available = reward.available && points.balance != null && points.balance >= reward.cost
                            isEnabled = available && !actions.isBusy
                            actionButtons += this to available
                            setOnClickListener {
                                if (!actions.confirm(context.tokenPrefs().getString(C.USER_ID, null))) return@setOnClickListener
                                updateActions()
                                confirmation = confirm(fragment, reward, onCancel = {
                                    actions.cancelConfirmation()
                                    updateActions()
                                }) { input ->
                                    if (!dialog.isShowing || !actions.submit(context.tokenPrefs().getString(C.USER_ID, null))) {
                                        actions.cancelConfirmation()
                                        updateActions()
                                        return@confirm
                                    }
                                    owner.lifecycleScope.launch {
                                        try {
                                            check(sameAccount())
                                            val result = repository.redeemViewerReward(network, headers, channelId, reward, input, UUID.randomUUID().toString())
                                            check(ViewerPoints.redemptionConfirmed(result))
                                            if (sameAccount() && dialog.isShowing) {
                                                android.widget.Toast.makeText(context, "Reward redeemed", android.widget.Toast.LENGTH_SHORT).show()
                                                refresh()
                                            }
                                        } catch (e: CancellationException) { throw e } catch (_: Exception) { if (dialog.isShowing) failure() }
                                        finally { actions.finish(); updateActions() }
                                    }
                                }
                            }
                        })
                    }
                } catch (e: CancellationException) { throw e } catch (_: Exception) {
                    if (dialog.isShowing && sameAccount()) {
                        message(context.getString(R.string.connection_error))
                        content.addView(Button(context).apply {
                            text = context.getString(R.string.refresh)
                            setOnClickListener { refresh() }
                        })
                    }
                }
            }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { dialog.dismiss() }
        }
        owner.lifecycle.addObserver(observer)
        dialog.setOnDismissListener {
            loading?.cancel()
            confirmation?.dismiss()
            owner.lifecycle.removeObserver(observer)
            actionButtons.clear()
            content.removeAllViews()
        }
        refresh()
    }

    private fun confirm(fragment: Fragment, reward: ViewerReward, onCancel: () -> Unit, submit: (String?) -> Unit): AlertDialog {
        val context = fragment.requireContext()
        val input = EditText(context).apply { hint = reward.prompt ?: "Reward message"; maxLines = 5 }
        val name = context.tokenPrefs().getString(C.USERNAME, null) ?: "active account"
        val builder = AlertDialog.Builder(context).setTitle("Redeem ${reward.title}?")
            .setMessage("Spend ${reward.cost} channel points as $name?" + (reward.prompt?.let { "\n\n$it" } ?: ""))
            .setNegativeButton(context.getString(R.string.hex_cancel), null).setPositiveButton(context.getString(R.string.hex_redeem), null)
        if (reward.inputRequired) builder.setView(input)
        val dialog = builder.create()
        var submitted = false
        dialog.setOnDismissListener { if (!submitted) onCancel() }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = input.text.toString().trim()
                if (reward.inputRequired && text.isBlank()) input.error = context.getString(R.string.hex_enter_message) else {
                    submitted = true
                    dialog.dismiss()
                    submit(if (reward.inputRequired) text else null)
                }
            }
        }
        dialog.show()
        return dialog
    }
}
