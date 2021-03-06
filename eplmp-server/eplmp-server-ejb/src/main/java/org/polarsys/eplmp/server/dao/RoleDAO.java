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
package org.polarsys.eplmp.server.dao;


import org.polarsys.eplmp.core.common.User;
import org.polarsys.eplmp.core.common.UserGroup;
import org.polarsys.eplmp.core.exceptions.CreationException;
import org.polarsys.eplmp.core.exceptions.RoleAlreadyExistsException;
import org.polarsys.eplmp.core.exceptions.RoleNotFoundException;
import org.polarsys.eplmp.core.workflow.Role;
import org.polarsys.eplmp.core.workflow.RoleKey;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Morgan Guimard
 */
public class RoleDAO {

    private EntityManager em;
    private Locale mLocale;

    private static final Logger LOGGER = Logger.getLogger(RoleDAO.class.getName());

    public RoleDAO(Locale pLocale, EntityManager pEM) {
        mLocale=pLocale;
        em=pEM;
    }

    public RoleDAO(EntityManager pEM) {
        mLocale=Locale.getDefault();
        em=pEM;
    }

    public Role loadRole(RoleKey roleKey) throws RoleNotFoundException {

        Role role = em.find(Role.class, roleKey);
        if (role == null) {
            throw new RoleNotFoundException(mLocale, roleKey);
        } else {
            return role;
        }

    }

    public List<Role> findRolesInWorkspace(String pWorkspaceId){
        return em.createNamedQuery("Role.findByWorkspace",Role.class).setParameter("workspaceId", pWorkspaceId).getResultList();
    }

    public void createRole(Role pRole) throws CreationException, RoleAlreadyExistsException {
        try{
            em.persist(pRole);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            LOGGER.log(Level.FINEST,null,pEEEx);
            throw new RoleAlreadyExistsException(mLocale, pRole);
        } catch (PersistenceException pPEx) {
            LOGGER.log(Level.FINEST,null,pPEx);
            throw new CreationException(mLocale);
        }
    }

    public void deleteRole(Role pRole){
        em.remove(pRole);
        em.flush();
    }

    public boolean isRoleInUseInWorkflowModel(Role role) {
        return !em.createNamedQuery("Role.findRolesInUseByRoleName")
                 .setParameter("roleName", role.getName())
                 .setParameter("workspace", role.getWorkspace())
                 .getResultList().isEmpty();

    }

    public List<Role> findRolesInUseWorkspace(String pWorkspaceId) {
        return em.createNamedQuery("Role.findRolesInUse",Role.class).setParameter("workspaceId", pWorkspaceId).getResultList();
    }

    public void removeUserFromRoles(User pUser) {
        List<Role> roles = em.createNamedQuery("Role.findRolesWhereUserIsAssigned", Role.class)
                .setParameter("user", pUser)
                .getResultList();
        for(Role role:roles){
            role.removeUser(pUser);
        }
        em.flush();
    }

    public void removeGroupFromRoles(UserGroup pGroup) {
        List<Role> roles = em.createNamedQuery("Role.findRolesWhereGroupIsAssigned", Role.class)
                .setParameter("userGroup", pGroup)
                .getResultList();
        for(Role role:roles){
            role.removeUserGroup(pGroup);
        }
        em.flush();
    }
}
