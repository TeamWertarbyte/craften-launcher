/**
 * CraftenLauncher is an alternative Launcher for Minecraft developed by Mojang.
 * Copyright (C) 2013  Johannes "redbeard" Busch, Sascha "saschb2b" Becker
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author redbeard
 */
package de.craften.craftenlauncher.logic;

import de.craften.craftenlauncher.exception.*;
import de.craften.craftenlauncher.logic.auth.AuthenticationService;
import de.craften.craftenlauncher.logic.auth.MinecraftUser;
import de.craften.craftenlauncher.logic.auth.Profiles;
import de.craften.craftenlauncher.logic.download.DownloadService;
import de.craften.craftenlauncher.logic.download.DownloadTasks;
import de.craften.craftenlauncher.logic.minecraft.MinecraftInfo;
import de.craften.craftenlauncher.logic.minecraft.MinecraftPathImpl;
import de.craften.craftenlauncher.logic.minecraft.MinecraftProcess;
import de.craften.craftenlauncher.logic.version.MinecraftVersion;
import de.craften.craftenlauncher.logic.version.VersionListHelper;
import de.craften.craftenlauncher.logic.vm.DownloadVM;
import de.craften.craftenlauncher.logic.vm.SkinVM;
import de.craften.util.UIParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observer;

public class LogicController {
    private static final Logger LOGGER = LogManager.getLogger(LogicController.class);
    private MinecraftUser mUser;
    private AuthenticationService mAuthService;
    private DownloadService mDownService;
    private UIParser mParser;
    private VersionListHelper mVersionList;
    private MinecraftPathImpl mMinecraftPath;
    private MinecraftVersion mCurrentVersion;
    private HashMap<String, String> mMincraftArgs;
    private Profiles mProfiles;
    private boolean mQuickPlay, mForceLogin, mFullscreen;

    private DownloadVM mDownloadVM;
    private SkinVM mSkinVM;

    public LogicController() {
        logLauncherVersion();
        mAuthService = new AuthenticationService();
        mProfiles = new Profiles();
        mDownloadVM = new DownloadVM();
        mMincraftArgs = new HashMap<>();
    }

    private void logLauncherVersion() {
        Package objPackage = this.getClass().getPackage();
        LOGGER.info("Launcher version: " + objPackage.getSpecificationVersion());
    }

    /**
     * Inits the logic layer.
     */
    public void init() {
        LOGGER.debug(mParser.toString());
        if (mParser.hasValue("mcpath")) {
            mMinecraftPath = new MinecraftPathImpl(mParser.getValue("mcpath"));
        } else {
            mMinecraftPath = new MinecraftPathImpl();
        }

        mProfiles.setPath(mMinecraftPath.getMinecraftDir());

        if (mParser.hasValue("server")) {
            mMincraftArgs.put("server", mParser.getValue("server"));
        }

        if (mParser.hasValue("xmx")) {
            mMincraftArgs.put("xmx", mParser.getValue("xmx"));
        }

        mVersionList = new VersionListHelper(mMinecraftPath);

        if (mParser.hasValue("version")) {
            String version = mParser.getValue("version");

            try {
                if (mVersionList.isVersionAvailableOnline(version)) {
                    this.mCurrentVersion = new MinecraftVersion(version);
                } else {
                    mVersionList.checkVesion(version);
                    this.mCurrentVersion = new MinecraftVersion(version);
                }

                mMincraftArgs.put("version", "true");
                mMinecraftPath.setVersionName(version);
            } catch (CraftenVersionNotKnownException e) {
                LOGGER.error("Version not available: " + version, e);
            }
        }

        if (mParser.hasKey("quickplay")) {
            mQuickPlay = true;
        }

        if (mParser.hasKey("forcelogin")) {
            mForceLogin = true;
        }

        if (mParser.hasKey("fullscreen")) {
            mFullscreen = true;
        }

        mAuthService.setMcPath(mMinecraftPath);
        Profiles login = mAuthService.readProfiles();

        if (login != null) {
            LOGGER.info("craftenlauncher_profiles.json found! Username is: " + login.getSelectedUser().getUsername());

            if (mParser.hasValue("profileid")) {
                login.changeSelectedUser(mParser.getValue("profileid"));
            }

            //TODO checken was genau die Response ist und was man damit so anfaengt!
            //user.setAuthentication(showProfile.getUsername(), null, showProfile.getAccessToken(), showProfile.getClientToken(), showProfile.getProfileId(),null);

            mProfiles = login;
        } else {
            LOGGER.info("No craftenlauncher_profiles.json found in " + mMinecraftPath.getMinecraftDir());
        }
    }

    public void setUser(String username, char[] password) throws CraftenLogicValueIsNullException {
        //TODO weiter char[] durchziehen wegen Sicherheit.
        String pass = String.valueOf(password);

        if (username == null || username.equals(" ") || username.equals("")) {
            LOGGER.error("Username is missing");
            throw new CraftenLogicValueIsNullException("Username is missing");
        }
        if (pass.equals(" ") || pass.equals("")) {
            LOGGER.error("Password is missing");
            throw new CraftenLogicValueIsNullException("Password is missing");
        }
        mProfiles.setSelectedUser(new MinecraftUser(username, pass));
    }

    public MinecraftUser getUser() throws CraftenLogicException {
        if (mProfiles.getSelectedUser() != null) {
            return mProfiles.getSelectedUser();
        } else {
            LOGGER.error("No user selected");
            throw new CraftenUserException("No user selected");
        }
    }

    public void authenticateUser() throws CraftenException {
        LOGGER.debug("Authenticate!");
        String session;

        if (mProfiles.getSelectedUser().hasAccessToken()) {                                                           // existing user
            session = mAuthService.getSessionID(mProfiles.getSelectedUser());
            if (session != null) {
                mProfiles.getSelectedUser().loggingInSuccess();

                if (mProfiles.getAvailableUser(mProfiles.getSelectedUser().getProfileId()) != null)
                    mProfiles.removeAvailableUser(mProfiles.getSelectedUser().getProfileId());
                mProfiles.addAvailableUser(mProfiles.getSelectedUser());

                mProfiles.save();
            }
        } else {      //new User
                session = mAuthService.getSessionID(mProfiles.getSelectedUser());

            if (session != null) {
                mProfiles.setSelectedUser(mAuthService.getUser());
                mProfiles.getSelectedUser().loggingInSuccess();

                if (mProfiles.getAvailableUser(mProfiles.getSelectedUser().getProfileId()) != null)
                    mProfiles.removeAvailableUser(mProfiles.getSelectedUser().getProfileId());
                mProfiles.addAvailableUser(mProfiles.getSelectedUser());

                mProfiles.save();
            }
        }

        startDownloadService();
        if (mDownService != null) {
            mDownService.downloadSkin(mSkinVM, mProfiles.getSelectedUser().getUsername());
        }
    }

    private void startDownloadService() throws CraftenLogicException {
        if (mDownService != null) {
            return;
        }

        if (!mProfiles.getSelectedUser().isLoggedIn()) {
            LOGGER.error("Trying to start DownloadService although user is not logged in!");
        }

        mDownService = new DownloadService(mMinecraftPath, mDownloadVM);

        if (mCurrentVersion != null) {
            try {
                mDownService.setMinecraftVersion(mCurrentVersion);
            } catch (Exception e) {
                //TODO Workaround vllt. klappt es beim zweiten Mal.
                LOGGER.info("Trying again to download json!");
                mDownService.setMinecraftVersion(mCurrentVersion);
            }
            mDownService.addTask(DownloadTasks.ressources);
            mDownService.addTask(DownloadTasks.jar);
            mDownService.addTask(DownloadTasks.libraries);
        }

        new Thread(mDownService).start();
    }

    public void logout() {
        LOGGER.info("Trying to logout user: " + mProfiles.getSelectedUser().getUsername());

        String name = mProfiles.getSelectedUser().getUsername();

        mAuthService.invalidate(mProfiles.getSelectedUser());
        if (mProfiles.getAvailableUser(mProfiles.getSelectedUser().getProfileId()) != null)
            mProfiles.removeAvailableUser(mProfiles.getSelectedUser().getProfileId());

        mProfiles.clearSelectedUser();
        mProfiles.save();
        // TODO: Muss das unbedingt null werden?
        // mCurrentVersion = null;
        mDownloadVM.setProgressBarToNull();
        //mAuthService.deleteProfiles();
        mAuthService = new AuthenticationService();

        if (mDownService != null) {
            LOGGER.debug("Shutting down DownloadService because of logout");
            mDownService.setRunning(false);

            mDownService = null;
        }
        LOGGER.info("User " + name + " has logged out");
    }

    //TODO vorher besser alten Service ordentlich stoppen oder neu Init?
    public void setMinecraftVersion(String version) throws CraftenLogicException {
        this.mVersionList.checkVesion(version);
        this.mCurrentVersion = new MinecraftVersion(version);

        mMinecraftPath.setVersionName(version);
        if (mDownService != null) {
            mDownService.setRunning(false);

            mDownService = null;
        }

        startDownloadService();
    }

    public MinecraftVersion getMinecraftVersion() throws CraftenLogicException {
        if (mCurrentVersion == null) {
            throw new CraftenLogicException();
        }
        return mCurrentVersion;
    }

    public ArrayList<String> getMinecraftVersions() {
        return mVersionList.getVersionsList();
    }

    public boolean isMinecraftDownloaded() {
        return mDownService.isFinished();
    }

    public boolean isQuickPlay() {
        return mQuickPlay;
    }

    public boolean isForceLogin() {
        return mForceLogin;
    }

    public void startMinecraft() throws CraftenLogicException {
        if (!isMinecraftDownloaded()) {
            LOGGER.error("Minecraft has not been downloaded fully!");
            throw new CraftenLogicException("Minecraft has not been downloaded fully yet!");
        }

        if (!mProfiles.getSelectedUser().isLoggedIn()) {
            LOGGER.error("Trying to start Minecraft although User is not logged in!");
            throw new CraftenLogicException("Trying to start Minecraft although User is not logged in!");
        }

        MinecraftInfo info = new MinecraftInfo(mCurrentVersion.getVersion());
        info.setUser(mProfiles.getSelectedUser());
        info.setMSV(mCurrentVersion);
        info.setMinecraftPath(mMinecraftPath);
        info.setXMX(mMincraftArgs.get("xmx"));
        //TODO Server und Port Anfrage ueberpruefen und falls noetig korrigieren.
        String server = mMincraftArgs.get("server");
        info.setServerAdress(server);
        info.setFullscreen(mFullscreen);

        MinecraftProcess process = new MinecraftProcess(info, info.getMSV().getVersionJson());

        process.startMinecraft();

        if (!process.getSuccess()) {
            LOGGER.error("Minecraft process could not be started!");
            throw new CraftenLogicException("Minecraft Process could not be started!");
        } else {
            System.exit(0);
        }
    }

    public void setParser(UIParser parser) throws CraftenLogicValueIsNullException {
        if (parser == null) {
            LOGGER.error("UI Parser was null!");
            throw new CraftenLogicValueIsNullException("Parser must not be null");
        }
        this.mParser = parser;
    }

    public void setDownloadObserver(Observer server) {
        mDownloadVM.addObserver(server);
    }

    public HashMap<String, String> getMinecraftArguments() {
        return new HashMap<String, String>(mMincraftArgs);
    }

    public String getMinecraftArgument(String key) throws CraftenLogicException {
        String argument = mMincraftArgs.get(key);

        if (argument != null) {
            return mMincraftArgs.get(key);
        } else {
            LOGGER.error("No argument for key: " + key);
            throw new CraftenLogicException("No Argument for key: " + key);
        }
    }

    //TODO Sicherheits-Checks mit einbauen.
    public void setMinecraftArguments(String key, String value) {
        mMincraftArgs.put(key, value);
    }

    public void setSkinObserver(Observer server) {
        mSkinVM = new SkinVM();
        mSkinVM.addObserver(server);
    }

    public void setSelectedUser(MinecraftUser user) {
        mProfiles.setSelectedUser(user);
    }

    /**
     * Returns a list of users from the current craftenlauncher_profiles.
     *
     * @return
     */
    public List<MinecraftUser> getUsers() {
        return mProfiles.getAvailableUsers();
    }
}
