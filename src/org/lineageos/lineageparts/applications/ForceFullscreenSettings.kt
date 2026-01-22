/*
 * SPDX-FileCopyrightText: 2026 AxionOS
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.applications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.android.axion.compose.theme.AxionTheme
import androidx.fragment.app.Fragment
import org.lineageos.lineageparts.R

class ForceFullscreenSettings : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AxionTheme {
                    ForceFullscreenScreen()
                }
            }
        }
    }
    

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.long_screen_settings_title)
    }
}
