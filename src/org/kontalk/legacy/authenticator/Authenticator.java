/*
 * Kontalk Android client
 * Copyright (C) 2014 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.legacy.authenticator;

import org.kontalk.legacy.R;
import org.kontalk.legacy.ui.NumberValidation;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


/**
 * The authenticator.
 * @author Daniele Ricci
 * @version 1.0
 */
public class Authenticator extends AbstractAccountAuthenticator {
    private static final String TAG = Authenticator.class.getSimpleName();

    public static final String ACCOUNT_TYPE = "org.kontalk.legacy.account";
    public static final String AUTHTOKEN_TYPE = "org.kontalk.legacy.token";

    private final Context mContext;
    private final Handler mHandler;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static String getDefaultAccountToken(Context ctx) {
        Account a = getDefaultAccount(ctx);
        if (a != null) {
            try {
                AccountManager m = AccountManager.get(ctx);
                return m.blockingGetAuthToken(a, AUTHTOKEN_TYPE, true);
            }
            catch (Exception e) {
                Log.e(TAG, "unable to retrieve default account token", e);
            }
        }
        Log.e(TAG, "default account NOT FOUND!");
        return null;
    }

    public static Account getDefaultAccount(Context ctx) {
        AccountManager m = AccountManager.get(ctx);
        Account[] accs = m.getAccountsByType(ACCOUNT_TYPE);
        return (accs.length > 0) ? accs[0] : null;
    }

    public static String getDefaultAccountName(Context ctx) {
        Account acc = getDefaultAccount(ctx);
        return (acc != null) ? acc.name : null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
            String accountType, String authTokenType,
            String[] requiredFeatures, Bundle options) throws NetworkErrorException {

        final Bundle bundle = new Bundle();

        if (getDefaultAccount(mContext) != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.only_one_account_supported,
                            Toast.LENGTH_LONG).show();
                }
            });
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_CANCELED);
        }
        else {
            final Intent intent = new Intent(mContext, NumberValidation.class);
            intent.putExtra(NumberValidation.PARAM_AUTHTOKEN_TYPE, authTokenType);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        }

        return bundle;
    }

    /**
     * System is requesting to confirm our credentials - this usually means that
     * something has changed (e.g. new SIM card), so we simply delete the
     * account for safety.
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
            Account account, Bundle options) throws NetworkErrorException {

        Log.v(TAG, "confirming credentials");
        // remove account
        AccountManager man = AccountManager.get(mContext);
        man.removeAccount(account, null, null);

        final Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
            String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {

        //Log.v(TAG, "auth token requested");
        if (!authTokenType.equals(AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE,
                "invalid authTokenType");
            return result;
        }
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null && password.length() > 0) {
            // exposing sensitive data - Log.v(TAG, "returning configured password: " + password);
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, password);
            return result;
        }

        Log.w(TAG, "token not found, deleting account");
        // incorrect or missing password - remove account
        AccountManager man = AccountManager.get(mContext);
        man.removeAccount(account, null, null);

        final Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (authTokenType.equals(AUTHTOKEN_TYPE)) {
            return mContext.getString(R.string.app_name);
        }
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
            Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

}