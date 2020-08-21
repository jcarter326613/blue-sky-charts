package com.blueskycharts.app

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.loader.ResourcesLoader
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.CheckBox
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.blueskycharts.app.map.MapViewActivity
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.utility.Log
import kotlin.system.exitProcess

class ConsentFragment : Fragment() {
    private var consentButton: Button? = null
    private var quitButton: Button? = null
    private var termsCheckbox: CheckBox? = null
    private var privacyCheckbox: CheckBox? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_consent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get all the view handles
        val privacyWebView = view.findViewById<WebView>(R.id.privacyWebView)
        consentButton = view.findViewById(R.id.acceptPolicyButton)
        quitButton = view.findViewById(R.id.closeAppButton)
        termsCheckbox = view.findViewById(R.id.termsConsent)
        privacyCheckbox = view.findViewById(R.id.privacyConsent)
        updateButtonColors()

        // Default the terms view
        privacyWebView.loadData(defaultTermsOfService, "text/html", "UTF-8")

        // Setup the consent checkboxes
        termsCheckbox?.setOnCheckedChangeListener { _, b ->
            if (b) {
                privacyWebView.loadData(defaultPrivacyPolicy, "text/html", "UTF-8")
                privacyCheckbox?.isEnabled = true
            } else {
                privacyWebView.loadData(defaultTermsOfService, "text/html", "UTF-8")
                privacyCheckbox?.isEnabled = false
            }
        }

        privacyCheckbox?.setOnCheckedChangeListener { _, _ ->
            updateButtonColors()
        }

        // Setup the consent and decline buttons
        consentButton?.setOnClickListener {
            if (privacyCheckbox?.isChecked == true && termsCheckbox?.isChecked == true) {
                acceptPolicy()
            } else {
                val alertDialog = AlertDialog.Builder(context)
                .setMessage("You must consent to the Terms of Service and Privacy Policy before continuing.")
                .setPositiveButton("I understand") { _: DialogInterface, _: Int ->
                }
                .create()
                alertDialog.show()
            }
        }

        quitButton?.setOnClickListener {
            val activity = this.activity
            if (activity != null) {
                activity.finish()
            } else {
                exitProcess(0)
            }
        }

        // Setup the firebase consent checkbox
        val firebaseConsentCheckmark = view.findViewById<CheckBox>(R.id.firebaseConsent)
        val firebaseConsent = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseLogging, Preferences.defaultValueAllowFirebaseLogging)
        val crashalyticsConsent = Preferences.instance.getBooleanValue(Preferences.propertyNameAllowFirebaseCrashalytics, Preferences.defaultValueAllowFirebaseCrashalytics)
        firebaseConsentCheckmark.isChecked = firebaseConsent && crashalyticsConsent
    }

    private fun acceptPolicy() {
        Preferences.instance.setPreference(Preferences.propertyNameAcceptedPrivacyVersion, privacyPolicyVersion)
        Preferences.instance.setPreference(Preferences.propertyNameAcceptedTermsOfServiceVersion, termsOfServiceVersion)

        val firebaseConsentCheckmark = view?.findViewById<CheckBox>(R.id.firebaseConsent)
        val firebaseConsent = firebaseConsentCheckmark?.isChecked ?: false

        Preferences.instance.setPreference(Preferences.propertyNameAllowFirebaseLogging, firebaseConsent)
        Preferences.instance.setPreference(Preferences.propertyNameAllowFirebaseCrashalytics, firebaseConsent)

        context?.let {
            Log.refreshFirebaseConsent(it)

            val intent = Intent(it, MapViewActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    private fun updateButtonColors() {
        val consentButton = this.consentButton ?: return
        val quitButton = this.quitButton ?: return

        ViewCompat.setBackgroundTintList(quitButton, ContextCompat.getColorStateList(quitButton.context, R.color.colorButtonSecondary))
        quitButton.setTextColor(ContextCompat.getColor(quitButton.context, R.color.colorButtonTextSecondary))

        if (privacyCheckbox?.isChecked == true && termsCheckbox?.isChecked == true) {
            ViewCompat.setBackgroundTintList(consentButton, ContextCompat.getColorStateList(consentButton.context, R.color.colorButtonPrimary))
            consentButton.setTextColor(ContextCompat.getColor(consentButton.context, R.color.colorButtonTextPrimary))
        } else {
            ViewCompat.setBackgroundTintList(consentButton, ContextCompat.getColorStateList(consentButton.context, R.color.colorButtonSecondary))
            consentButton.setTextColor(ContextCompat.getColor(consentButton.context, R.color.colorButtonTextSecondary))
        }
    }

    companion object {
        private const val defaultPrivacyPolicy = "<html>     <body>         <h1>PRIVACY POLICY</h1>         <p>             This privacy policy governs your use of the software application Blue Sky Charts (\"Application\")              that was created by Muddy Paw Cloud Technology LLC (\"Company\"). The Application is an informational tool used for flight planning.         </p>          <h2>Automatically Collected Information</h2>         <p>             The Application may collect certain information automatically including the IP address of your mobile device and information about the way you use the Application when retrieving information from a server.  Identifying information will not be collected by the Application other than the IP address of the mobile device while the Application is running and connected to the Internet.  Instances where this data is collected include, but are not limited to, requesting weather information, map images, etc.         </p>          <p>             The Application makes use of the following components of Firebase, a product offered by Google, which collect user information and application usage data.         </p>         <ul>             <li>Google Analytics - Used to collect usage data within the Application.  This helps us learn what features are being used and what paths users are taking through our application.  This information helps us improve the Application in future versions.</li>             <li>Crashalytics - Used to collect information when the Application crashes on a user's device.  We are sent details about what the state of the device was at the time of the crash and full stack traces for every thread running within the Application at the time of the crash.  This helps us identify the existence and cause of some errors being experienced by our customers so that we may fix them in future versions of the Application.</li>         </ul>          <h2>What are my opt-out rights?</h2>         <p>             To stop all data collection by the Application, you must uninstall it.  You may use the standard uninstall processes as may be available as part of your mobile device or via the mobile application marketplace or network.  To prevent data collection for just Firebase associated data, follow the steps below.         </p>         <ul>             <li>Google Analytics and Crashalytics - On the map screen, click the menu button, followed by Settings and then Privacy.  Follow the instructions on the screen and then click Save Changes.</li>         </ul>          <h2>How does Google use the data they collect through my use of this Application?</h2>         <p>             Information on how Google uses data provided to them through them as a result of the Application's use of Firebase can be found at the following location.         </p>         <p><a href=\"https://policies.google.com/technologies/partner-sites\" target=\"_blank\">https://policies.google.com/technologies/partner-sites</a></p>          <h2>Data Retention Policy</h2>         <p>             We will retain collected information until such time as the information is deemed irrelevant by the Company for the purposes of improving the Application or any other purpose.  To have Firebase related data deleted, follow Google's data deletion process.         </p>         <p><a href=\"https://policies.google.com/technologies/retention\" target=\"_blank\">https://policies.google.com/technologies/retention</a></p>          <h2>Security</h2>         <p>             We provide physical, electronic, and procedural safeguards to protect information we process and maintain. For example, we limit access to this information to authorized employees and contractors who need to know that information in order to operate, develop or improve our Application. Please be aware that, although we endeavor to provide reasonable security for information we process and maintain, no security system can prevent all potential security breaches.         </p>          <h2>Changes</h2>         <p>             This Privacy Policy may be updated from time to time for any reason. We will notify you of any changes to our Privacy Policy by posting the new Privacy Policy here and informing you on launch of the Application when connected to the Internet. You are advised to consult this Privacy Policy regularly for any changes, as continued use is deemed approval of all changes.         </p>          <h2>Your Consent</h2>         <p>             By using the Application, you are consenting to our processing of your information as set forth in this Privacy Policy now and as amended by us. \"Processing,\" means using cookies on a computer/hand held device or using or touching information in any way, including, but not limited to, collecting, storing, deleting, using, combining and disclosing information, all of which activities will take place in the United States. If you reside outside the United States your information will be transferred, processed and stored there under United States privacy standards.          </p>                  <h2>Contact us</h2>         <p>             If you have any questions regarding privacy while using the Application, or have questions about our practices, please contact us via email at muddypawcloudtechnology@gmail.com.         </p>     </body> </html>"
        private const val defaultTermsOfService = "<html>     <body>         <h1>Terms of Service</h1>   </body> </html>"
        private const val privacyPolicyVersion = "2020-07-28"
        private const val termsOfServiceVersion = "2020-07-28"
        val policyAccepted: Boolean
            get() {
                val acceptedVersion = Preferences.instance.getStringValue(Preferences.propertyNameAcceptedPrivacyVersion, Preferences.defaultValueAcceptedPrivacyVersion)
                return acceptedVersion == privacyPolicyVersion
            }
        val termsAccepted: Boolean
            get() {
                val acceptedVersion = Preferences.instance.getStringValue(Preferences.propertyNameAcceptedTermsOfServiceVersion, Preferences.defaultValueAcceptedTermsOfServiceVersion)
                return acceptedVersion == termsOfServiceVersion
            }
    }
}