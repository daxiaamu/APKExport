package cn.leftshine.apkexport.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import cn.leftshine.apkexport.R

class PackageInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirmation?.let(context::startActivity)
            }
            PackageInstaller.STATUS_SUCCESS -> Toast.makeText(context, R.string.local_package_install_success, Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(context, context.getString(R.string.local_package_install_failed, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()), Toast.LENGTH_LONG).show()
        }
    }

    companion object { const val ACTION_INSTALL_STATUS = "cn.leftshine.apkexport.action.INSTALL_STATUS" }
}