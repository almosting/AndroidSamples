package com.fengwei23.navigationsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions

/**
 * Fragment used to show how to navigate to another destination
 */
class HomeFragment : Fragment() {
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    setHasOptionsMenu(true)
    return inflater.inflate(R.layout.home_fragment, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    //Set NavOptions
    val options = navOptions {
      anim {
        enter = R.anim.slide_in_right
        exit = R.anim.slide_out_left
        popEnter = R.anim.slide_in_left
        popExit = R.anim.slide_out_right
      }
    }
    view.findViewById<Button>(R.id.navigate_destination_button)?.setOnClickListener {
      findNavController().navigate(R.id.flow_step_one_dest, null, options)
    }

    //Update the OnClickListener to navigate using an action
    view.findViewById<Button>(R.id.navigate_action_button)?.setOnClickListener {
      val flowStepNumberArg = 1
      val action = HomeFragmentDirections.nextAction(flowStepNumberArg)
      findNavController().navigate(action)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.main_menu, menu)
  }
}
