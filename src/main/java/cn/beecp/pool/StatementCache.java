/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.pool;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static cn.beecp.util.BeecpUtil.oclose;

/**
 * Statement cache
 *
 * @author Chris.liao
 * @version 1.0
 */
class StatementCache {
	private int capacity;
	private CacheNode head=null;//old
	private CacheNode tail=null;//new
	private HashMap<Object,CacheNode>nodeMap;
	public StatementCache(int capacity) {
		this.capacity=capacity;
		this.nodeMap = new HashMap<Object,CacheNode>(capacity*2);
	}
	public PreparedStatement getStatement(Object k) {
		CacheNode n = nodeMap.get(k);
		if(n == null) return null;

		if(nodeMap.size()>1 && n!=tail) {
			//remove from chain
			if (n == head) {//at head
				head = head.next;
			} else {//at middle
				n.pre.next = n.next;
				n.next.pre = n.pre;
			}
			//append to tail
			tail.next = n;
			n.pre = tail;
			n.next = null;
			tail = n;
		}
		return n.v;
	}
	public void putStatement(Object k,PreparedStatement v) {
		CacheNode n = new CacheNode(k, v);
		nodeMap.put(k, n);
		if (head == null) {
			tail = head = n;
		} else {
			tail.next = n;
            n.pre = tail;
            tail = n;

			if (nodeMap.size() > capacity) {
				nodeMap.remove(head.k);
				oclose(head.v);
				if (head == tail) {
					head = null;
					tail = null;
				} else {
					head = head.next;
				}
			}
		}
	}
	void clearStatement() {
		Iterator<Map.Entry<Object, CacheNode>> itor=nodeMap.entrySet().iterator();
		while (itor.hasNext()) {
			Map.Entry<Object,CacheNode> entry=itor.next();
			itor.remove();
			oclose(entry.getValue().v);
		}

		head=null;
		tail=null;
	}

	static class CacheNode {// double linked chain node
		Object k;
		PreparedStatement v;
		CacheNode pre;
		CacheNode next;
		CacheNode(Object k, PreparedStatement v) {
			this.k = k;
			this.v = v;
		}
	}
}
class StatementCachePsKey{
	private String sql;
	private int autoGeneratedKeys;
	private int[] columnIndexes=null;
	private String[] columnNames=null;
	private int resultSetType=0;
	private int resultSetConcurrency=0;
	private int resultSetHoldability=0;

	private final static int prime=31;
	private final static int TYPE1=1;
	private final static int TYPE2=2;
	private final static int TYPE3=3;
	private final static int TYPE4=4;
	private final static int TYPE5=5;
	private final static int TYPE6=6;
	private int type=TYPE1;

	private int hashCode;
	public StatementCachePsKey(String sql) {
		this.sql=sql;
		hashCode=sql.hashCode();
	}
	public StatementCachePsKey(String sql, int autoGeneratedKeys) {
		this.type=TYPE2;
		this.sql=sql;
		this.autoGeneratedKeys=autoGeneratedKeys;

		hashCode=autoGeneratedKeys;
		hashCode=prime * hashCode+sql.hashCode();
	}
	public StatementCachePsKey(String sql, int[] columnIndexes) {
		this.type=TYPE3;
		this.sql=sql;
		this.columnIndexes=columnIndexes;

		hashCode=Arrays.hashCode(columnIndexes);
		hashCode=prime * hashCode+ sql.hashCode();;
	}
	public StatementCachePsKey(String sql, String[] columnNames) {
		this.type=TYPE4;
		this.sql=sql;
		this.columnNames=columnNames;

		hashCode=Arrays.hashCode(columnNames);
		hashCode=prime * hashCode+sql.hashCode();
	}
	public StatementCachePsKey(String sql, int resultSetType, int resultSetConcurrency) {
		this.type=TYPE5;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;

		hashCode=resultSetType;
		hashCode=prime * hashCode+resultSetConcurrency;
		hashCode=prime * hashCode+sql.hashCode();
	}
	public StatementCachePsKey(String sql, int resultSetType, int resultSetConcurrency,int resultSetHoldability) {
		this.type=TYPE6;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;
		this.resultSetHoldability=resultSetHoldability;

		hashCode=resultSetType;
		hashCode=prime * hashCode+resultSetConcurrency;
		hashCode=prime * hashCode+resultSetHoldability;
		hashCode=prime * hashCode+ +sql.hashCode();;
	}

	@Override
	public int hashCode(){
		return hashCode;
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof StatementCachePsKey))return false;
		StatementCachePsKey other=(StatementCachePsKey)obj;
		if(this.type!=other.type)return false;
		switch(this.type){
			case TYPE1:return this.sql.equals(other.sql);
			case TYPE2:return autoGeneratedKeys==other.autoGeneratedKeys && this.sql.equals(other.sql);
			case TYPE3:return Arrays.equals(columnIndexes,other.columnIndexes)&& this.sql.equals(other.sql);
			case TYPE4:return Arrays.equals(columnNames,other.columnNames)&& this.sql.equals(other.sql);
			case TYPE5:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && this.sql.equals(other.sql);
			case TYPE6:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && resultSetHoldability==other.resultSetHoldability && this.sql.equals(other.sql);
			default:return false;
		}
	}
}
class StatementCacheCsKey{
	private String sql;
	private int resultSetType=0;
	private int resultSetConcurrency=0;
	private int resultSetHoldability=0;

	private final static int prime=31;
	private final static int TYPE7=7;
	private final static int TYPE8=8;
	private final static int TYPE9=9;

	private int type=TYPE7;
	private int hashCode;
	public StatementCacheCsKey(String sql) {
		this.sql=sql;
		hashCode=sql.hashCode();
	}
	public StatementCacheCsKey(String sql, int resultSetType, int resultSetConcurrency) {
		this.type=TYPE8;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;

		hashCode=resultSetType;
		hashCode=prime * hashCode+ resultSetConcurrency;
		hashCode=prime * hashCode+ sql.hashCode();
	}
	public StatementCacheCsKey(String sql, int resultSetType, int resultSetConcurrency,int resultSetHoldability) {
		this.type=TYPE9;
		this.sql=sql;
		this.resultSetType=resultSetType;
		this.resultSetConcurrency=resultSetConcurrency;
		this.resultSetHoldability=resultSetHoldability;

		hashCode=resultSetType;
		hashCode=prime * hashCode+resultSetConcurrency;
		hashCode=prime * hashCode+resultSetHoldability;
		hashCode=prime * hashCode+ +sql.hashCode();;
	}
	@Override
	public int hashCode(){
		return hashCode;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof StatementCacheCsKey) {
			StatementCacheCsKey other = (StatementCacheCsKey)obj;
			if(this.type==other.type) {
				switch(this.type){
					case TYPE7:return this.sql.equals(other.sql);
					case TYPE8:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && this.sql.equals(other.sql);
					case TYPE9:return resultSetType==other.resultSetType && resultSetConcurrency==other.resultSetConcurrency && resultSetHoldability==other.resultSetHoldability && this.sql.equals(other.sql);
					default:return false;
				}
			}
		}
		return false;
	}
}