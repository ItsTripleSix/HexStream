package com.github.andreyasadchy.xtra.ui.common

import androidx.fragment.app.Fragment

interface FragmentHost {
    val currentFragment: Fragment?
}