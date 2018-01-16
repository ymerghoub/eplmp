/*******************************************************************************
  * Copyright (c) 2017 DocDoku.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    DocDoku - initial API and implementation
  *******************************************************************************/
package org.polarsys.eplmp.server;

import org.polarsys.eplmp.core.common.BinaryResource;
import org.polarsys.eplmp.core.exceptions.FileNotFoundException;
import org.polarsys.eplmp.core.exceptions.StorageException;
import org.polarsys.eplmp.core.security.UserGroupMapping;
import org.polarsys.eplmp.core.services.IBinaryStorageManagerLocal;
import org.polarsys.eplmp.server.storage.StorageProvider;
import org.polarsys.eplmp.server.storage.filesystem.FileStorageProvider;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@Local(IBinaryStorageManagerLocal.class)
@Stateless(name = "BinaryStorageManagerBean")
public class BinaryStorageManagerBean implements IBinaryStorageManagerLocal {

    @Inject
    private ConfigManager configManager;

    private StorageProvider defaultStorageProvider;

    @PostConstruct
    private void init() {
        StorageProvider fileStorageProvider = new FileStorageProvider(configManager.getVaultPath());
        defaultStorageProvider = fileStorageProvider;
    }

    @Override
    public InputStream getBinaryResourceInputStream(BinaryResource binaryResource) throws StorageException {
        try {
            return defaultStorageProvider.getBinaryResourceInputStream(binaryResource);
        } catch (FileNotFoundException e) {
            BinaryResource previous = binaryResource.getPrevious();
            if (previous != null) {
                return getBinaryResourceInputStream(previous);
            } else {
                throw new StorageException("Can't find " + binaryResource.getFullName());
            }
        }
    }

    @Override
    public OutputStream getBinaryResourceOutputStream(BinaryResource binaryResource) throws StorageException {
        return defaultStorageProvider.getBinaryResourceOutputStream(binaryResource);
    }

    @Override
    public InputStream getGeneratedFileInputStream(BinaryResource binaryResource, String generatedFileName) throws StorageException {
        try {
            return defaultStorageProvider.getGeneratedFileInputStream(binaryResource, generatedFileName);
        } catch (FileNotFoundException e) {
            BinaryResource previous = binaryResource.getPrevious();
            if (previous != null) {
                return getGeneratedFileInputStream(previous, generatedFileName);
            } else {
                throw new StorageException("Can't find generated file " + generatedFileName + " of " + binaryResource.getFullName());
            }
        }
    }

    @Override
    public OutputStream getGeneratedFileOutputStream(BinaryResource binaryResource, String generatedFileName) throws StorageException {
        return defaultStorageProvider.getGeneratedFileOutputStream(binaryResource, generatedFileName);
    }

    @Override
    public boolean exists(BinaryResource binaryResource, String generatedFileName) throws StorageException {
        if (defaultStorageProvider.exists(binaryResource, generatedFileName)) {
            return true;
        } else {
            BinaryResource previous = binaryResource.getPrevious();
            return previous != null && exists(previous, generatedFileName);
        }
    }

    @Override
    public Date getLastModified(BinaryResource binaryResource, String generatedFileName) throws StorageException {
        try {
            return defaultStorageProvider.getLastModified(binaryResource, generatedFileName);
        } catch (FileNotFoundException e) {
            BinaryResource previous = binaryResource.getPrevious();
            if (previous != null) {
                return getLastModified(previous, generatedFileName);
            } else {
                throw new StorageException("Can't find source file to get last modified date " + binaryResource.getFullName());
            }
        }
    }


    @Override
    public void copyData(BinaryResource source, BinaryResource destination) throws StorageException {
        try {
            defaultStorageProvider.copyData(source, destination);
        } catch (FileNotFoundException e) {
            BinaryResource previous = source.getPrevious();
            if (previous != null) {
                copyData(previous, destination);
            } else {
                throw new StorageException("Can't find source file to copy " + source.getFullName());
            }
        }
    }

    @Override
    public void deleteData(BinaryResource binaryResource) throws StorageException {
        defaultStorageProvider.delData(binaryResource);
    }

    private File getEffectiveBinaryResourceFile(BinaryResource binaryResource) throws StorageException {
        try {
            return defaultStorageProvider.getBinaryResourceFile(binaryResource);
        } catch (FileNotFoundException e) {
            BinaryResource previous = binaryResource.getPrevious();
            if (previous != null) {
                return getEffectiveBinaryResourceFile(previous);
            } else {
                throw new StorageException("Can't find resource " + binaryResource.getFullName());
            }
        }
    }

    @Override
    public void renameFile(BinaryResource binaryResource, String pNewName) throws StorageException, FileNotFoundException {
        //because we do not duplicate file from iteration to iteration if nothing has changed
        //file could be share among iterations
        File file = getEffectiveBinaryResourceFile(binaryResource);

        try {
            //hence we check if file is only used by the current BinaryResource
            defaultStorageProvider.getBinaryResourceFile(binaryResource);
        } catch (FileNotFoundException e) {
            //we have got an exception, that means file is shared
            file = defaultStorageProvider.copyFile(file, binaryResource);
        }

        defaultStorageProvider.renameData(file, pNewName);
    }

    @Override
    public String getExternalStorageURI(BinaryResource binaryResource) {
        return defaultStorageProvider.getExternalResourceURI(binaryResource);
    }

    @Override
    public String getShortenExternalStorageURI(BinaryResource binaryResource) {
        return defaultStorageProvider.getShortenExternalResourceURI(binaryResource);
    }

    @Override
    public void deleteWorkspaceFolder(String workspaceId) throws StorageException {
        defaultStorageProvider.deleteWorkspaceFolder(workspaceId);
    }

}
