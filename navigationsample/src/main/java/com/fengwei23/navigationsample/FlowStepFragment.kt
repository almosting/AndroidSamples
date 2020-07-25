package com.fengwei23.navigationsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs

/**
 * Presents how multiple steps flow could be implemented.
 */
class FlowStepFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    setHasOptionsMenu(true)

    //val flowStepNumber = arguments?.getInt("flowStepNumber")

    val safeArgs: FlowStepFragmentArgs by navArgs()

    return when (safeArgs.flowStepNumber) {
      2 -> inflater.inflate(R.layout.flow_step_two_fragment, container, false)
      else -> inflater.inflate(R.layout.flow_step_one_fragment, container, false)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<View>(R.id.next_button).setOnClickListener(
      Navigation.createNavigateOnClickListener(R.id.next_action)
    )
  }
}
