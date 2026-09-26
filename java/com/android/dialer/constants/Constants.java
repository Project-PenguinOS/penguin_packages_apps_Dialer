/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.constants;

/**
 * Utility to access constants
 */
public class Constants {
  public static final String APPLICATION_ID = "com.penguin.dialer";

  public static final String FILE_PROVIDER_AUTHORITY = APPLICATION_ID + ".files";

  public static final String ANNOTATED_CALL_LOG_PROVIDER_AUTHORITY =
          APPLICATION_ID + ".annotatedcalllog";

  public static final String PHONE_LOOKUP_HISTORY_PROVIDER_AUTHORITY =
          APPLICATION_ID + ".phonelookuphistory";

  public static final String PREFERRED_SIM_FALLBACK_PROVIDER_AUTHORITY =
          APPLICATION_ID + ".preferredsimfallback";

  public static final String LOOKUP_PROVIDER_AUTHORITY = APPLICATION_ID + ".lookup";

  public static final String PERMISSION_DIALER_ORIGIN =
          APPLICATION_ID + ".permission.DIALER_ORIGIN";
}
