/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.causalclustering.discovery;

import java.util.Set;

import org.neo4j.causalclustering.core.consensus.LeaderInfo;
import org.neo4j.causalclustering.core.consensus.LeaderListener;
import org.neo4j.causalclustering.core.consensus.RaftMachine;
import org.neo4j.causalclustering.identity.MemberId;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;

/**
 * Makes the Raft aware of changes to the core topology and vice versa
 */
public class RaftCoreTopologyConnector extends LifecycleAdapter implements CoreTopologyService.Listener, LeaderListener
{
    private final CoreTopologyService coreTopologyService;
    private final RaftMachine raftMachine;
    private final String dbName;

    public RaftCoreTopologyConnector( CoreTopologyService coreTopologyService, RaftMachine raftMachine, String dbName )
    {
        this.coreTopologyService = coreTopologyService;
        this.raftMachine = raftMachine;
        this.dbName = dbName;
    }

    @Override
    public void start()
    {
        coreTopologyService.addLocalCoreTopologyListener( this );
        raftMachine.registerListener( this );
    }

    @Override
    public synchronized void onCoreTopologyChange( CoreTopology coreTopology )
    {
        Set<MemberId> targetMembers = coreTopology.members().keySet();
        raftMachine.setTargetMembershipSet( targetMembers );
    }

    @Override
    public void onLeaderSwitch( LeaderInfo leaderInfo )
    {
        coreTopologyService.setLeader( leaderInfo, dbName );
    }

    @Override
    public void onLeaderStepDown( LeaderInfo leaderInfo )
    {
        coreTopologyService.handleStepDown( leaderInfo, dbName );
    }

    @Override
    public String dbName()
    {
        return this.dbName;
    }
}
