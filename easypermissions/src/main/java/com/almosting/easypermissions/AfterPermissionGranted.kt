package com.almosting.easypermissions

import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Created by w.feng on 2018/10/10
 * Email: fengweisb@gmail.com
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(
  FUNCTION,
  PROPERTY_GETTER,
  PROPERTY_SETTER
)
annotation class AfterPermissionGranted(val value: Int)