'''
Created on Apr 27, 2014

@author: crusherofheads

Calculate disk usage by user, separated into public vs. private and
deleted vs. undeleted data.

These figures are not actually related to the physical disk space for three
reasons:
1) The workspace saves space by only keeping one copy of each unique
    document. From the perspective of user disk usage, this feature is ignored.
2) Copies in the workspace are copies by reference, not by value. Again,
    from the perspective of user disk usage, this feature is ignored.
3) Only actual data objects are included (e.g. data stored in GridFS or Shock).
    Any data stored in MongoDB (other than GridFS files) is not included.

Don't run this during high loads - runs through every object in the DB
Hasn't been optimized much either
'''

# TODO: all this needs a rewrite
# TODO: public vs. private, deleted vs undeleted


from __future__ import print_function
from configobj import ConfigObj
from pymongo import MongoClient
import time
import sys
import os
from collections import defaultdict

# where to get credentials (don't check these into git, idiot)
DEFAULT_CFG_FILE = 'usage.cfg'
CFG_SECTION_SOURCE = 'SourceMongo'
CFG_SECTION_TARGET = 'TargetMongo'

HOST = 'host'
PORT = 'port'
USER = 'user'
PWD = 'pwd'

# collection names
COL_WS = 'workspaces'
COL_ACLS = 'workspaceACLs'
COL_OBJ = 'workspaceObjects'
COL_VERS = 'workspaceObjVersions'

PUBLIC = 'pub'
PRIVATE = 'priv'

LIMIT = 10000
OR_QUERY_SIZE = 100  # 75 was slower, 150 was slower
MAX_WS = -1  # for testing, set to < 1 for all ws


def chunkiter(iterable, size):
    """Iterates over an iterable in chunks of size size. Returns an iterator
  that in turn returns iterators over the iterable that each iterate through
  size objects in the iterable.
  Note that since the inner and outer loops are pulling values from the same
  iterator, continue and break don't necessarily behave exactly as one would
  expect. In the outer loop of the iteration, continue effectively does
  nothing, but break works normally. In the inner loop, break has no real
  effect but continue works normally. For the latter issue, wrapping the inner
  iterator in a tuple will cause break to skip the remaining items in the
  iterator. Alternatively, one can set a flag and exhaust the inner iterator.
  """
    def inneriter(first, iterator, size):
        yield first
        for _ in xrange(size - 1):
            yield iterator.next()
    it = iter(iterable)
    while True:
        yield inneriter(it.next(), it, size)


def process_optional_key(configObj, section, key):
    v = configObj[section].get(key)
    v = None if v == '' else v
    configObj[section][key] = v
    return v


def get_config():
    if len(sys.argv) > 1:
        cfgfile = sys.argv[1]
    else:
        cfgfile = DEFAULT_CFG_FILE
    if not os.path.isfile(cfgfile) and not os.access(cfgfile, os.R_OK):
        print ('Cannot read file ' + cfgfile)
        sys.exit(1)
    co = ConfigObj(cfgfile)
    s = CFG_SECTION_SOURCE
    t = CFG_SECTION_TARGET

    for sec in (s, t):
        if sec not in co:
            print('Missing config section {} from file {}'.format(
                  sec, cfgfile))
            sys.exit(1)
        for key in (HOST, PORT):
            v = co[sec].get(key)
            if v == '' or v is None:
                print('Missing config value {}.{} from file {}'.format(
                    sec, key, cfgfile))
                sys.exit(1)
    for sec in (s, t):
        u = process_optional_key(co, sec, USER)
        p = process_optional_key(co, sec, PWD)
        if u is not None and p is None:
            print ('If {} specified, {} must be specified in section '.format(
                USER, PWD) + '{} from file {}'.format(sec, cfgfile))
            sys.exit(1)
    return ConfigObj(cfgfile)[s], ConfigObj(cfgfile)[t]


def main():
    sourcecfg, targetcfg = get_config()
    print(sourcecfg)
    print(targetcfg)

if __name__ == '__main__':
    main()

# everything below here is not scottish

def process_objects(objs, unique_users, types, workspaces):
    objsproc = 0
    size = 0
    # note all objects are from the same workspace
    for objs in chunkiter(objs, OR_QUERY_SIZE):
        innerq = []
        for o in objs:
            innerq.append({'id': o['id'], 'ver': o['numver']})
        res = db[COL_VERS].find({'ws': o['ws'], '$or': innerq},
                                ['type', 'ws', 'savedby', 'size'])
        for v in res:
            unique_users.add(v['savedby'])
            size += v['size']
            tname, ver = v['type'].split('-')
            if tname not in types:
                types[tname] = {}
            if ver not in types[tname]:
                types[tname][ver] = {}
                types[tname][ver][PUBLIC] = 0
                types[tname][ver][PRIVATE] = 0
            p = PUBLIC if workspaces[v['ws']]['pub'] else PRIVATE
            types[tname][ver][p] += 1
            objsproc += 1
    return size, objsproc


if __name__ == '__main__':
    cfg = ConfigObj(CREDS_FILE)
    user = cfg[CREDS_SECTION][USER]
    pwd = cfg[CREDS_SECTION][PWD]
    starttime = time.time()
    mongo = MongoClient(WS_MONGO_HOST, WS_MONGO_PORT, slaveOk=True)
    db = mongo[WS_MONGO_DB]
    db.authenticate(user, pwd)
    # may need to do this in chunks in the future, for now there's
    # < 2000 workspaces
    ws_cursor = db[COL_WS].find({'del': False}, ['ws', 'numObj'])
    pub_read = db[COL_ACLS].find({'user': '*'}, ['id'])
    workspaces = defaultdict(dict)
    for ws in ws_cursor:
        workspaces[ws['ws']]['pub'] = False
        workspaces[ws['ws']]['numObj'] = ws['numObj']
    for pr in pub_read:
        if pr['id'] in workspaces:  # otherwise deleted
            workspaces[pr['id']]['pub'] = True
    print('Total workspaces: ' + str(len(workspaces)))
    print("Total objects: " + str(db[COL_OBJ].count()))
    types = {}
    wscount = 0
    objcount = 0
    unique_users = set()
    total_size = 0
    for ws in workspaces:
        if MAX_WS > 0 and wscount > MAX_WS:
            break
        wsobjcount = workspaces[ws]['numObj']
        print('\nProcessing workspace {}, {} objects'.format(ws, wsobjcount))
        for lim in xrange(LIMIT, wsobjcount + LIMIT, LIMIT):
            print('\tProcessing objects {} - {}'.format(
                lim - LIMIT + 1, wsobjcount if lim > wsobjcount else lim))
            sys.stdout.flush()
            objtime = time.time()
            query = {'del': False, 'ws': ws,
                     'id': {'$gt': lim - LIMIT, '$lte': lim}}
            objs = db[COL_OBJ].find(query, ['ws', 'id', 'numver'])
            print('\ttotal obj query time: ' + str(time.time() - objtime))
            ttlstart = time.time()

            size, objsproc = process_objects(
                objs, unique_users, types, workspaces)

            total_size += size
            print('\ttotal ver query time: ' + str(time.time() - ttlstart))
            print('\tobjects processed: ' + str(objsproc))
            objcount += objsproc
            print('total objects processed: ' + str(objcount))
            sys.stdout.flush()
        wscount += 1

    pubws = 0
    privws = 0
    for ws in workspaces:
        if workspaces[ws]['pub']:
            pubws += 1
        else:
            privws += 1

    print('\nElapsed time: ' + str(time.time() - starttime))

    print('\nResults:')
    print('Total public workspaces ' + str(pubws))
    print('Total private workspaces ' + str(privws))
    print('Total users who have saved or copied an object: ' +
          str(len(unique_users)))
    print('Total size of stored data (double counts copies and identical ' +
          'data saved > 1 times): ' + str(total_size))

    print('\n' + '\t'.join(['Type', 'Version', 'Public', 'Private', 'TTL']))
    pub_tot = 0
    priv_tot = 0
    for t in types:
        pub_type_tot = 0
        priv_type_tot = 0
        for v in sorted(types[t]):
            print('\t'.join([t, v, str(types[t][v][PUBLIC]),
                             str(types[t][v][PRIVATE]),
                             str(types[t][v][PUBLIC] + types[t][v][PRIVATE])]))
            pub_type_tot += types[t][v][PUBLIC]
            priv_type_tot += types[t][v][PRIVATE]
        print('\t'.join([t, 'TTL', str(pub_type_tot), str(priv_type_tot),
                         str(pub_type_tot + priv_type_tot)]))
        pub_tot += pub_type_tot
        priv_tot += priv_type_tot
    print('\t'.join(['TTL', '-', str(pub_tot), str(priv_tot)]))