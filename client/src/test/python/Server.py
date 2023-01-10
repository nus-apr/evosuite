import os.path
import socket
import json
from os.path import exists

HOST = "127.0.0.1"  # Standard loopback interface address (localhost)
PORT = 7777  # Port to listen on (non-privileged ports are > 1023)

# Example implementation of an Orchestrator-Server for testing purposes.
# Sends dummy data based on the requests made by the client.

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print('[Server] Waiting for connection.')
        conn, addr = s.accept()
        with conn:
            print(f"[Server] Connected to client at port: {addr}.")
            while True:
                data = conn.recv(1024)
                if not data:
                    break
                json_data = json.loads(data)
                cmd = json_data['cmd']

                if cmd == 'readJsonFile':
                    jsonFilePath = json_data['data']['path']
                    with open(jsonFilePath, 'r') as f:
                        json_data = json.loads(f.read())
                        cmd = json_data['cmd']
            
                if cmd == 'getPatchPool':
                    print('[Server] Sending patch pool.')
                    getPatchPool(conn)
                elif cmd == 'updateTestPopulation':
                    print('[Server] Updating test population.')
                    updateTestPopulation(json_data, conn)
                elif cmd == 'getPatchValidationResult':
                    print('[Server] Sending patch validation result.')
                    getPatchValidationResult(json_data, conn)
                elif cmd == 'closeConnection':
                    print('[Server] Closing connection.')
                    break
                else:
                    print(f"[Server] Unknown command: {cmd}. Sending back echo.")
                    if 'data' not in json_data.keys():
                        json_data['data'] = [4, 0, 4]
                    conn.sendall(bytes(json.dumps(json_data), 'UTF-8'))

            print('[Server] Connection closed.')

# Sends a dummy patch pool consisting of 10 patches to the client.
def getPatchPool(conn):
    patch_pool = [{"id": i} for i in range(10)]
    reply = {
            "cmd": "getPatchPool",
            "data": patch_pool
            }
    
    conn.sendall(bytes(json.dumps(reply), 'UTF-8'))

# Reads population data, checks if provided file exists, and sends back 
# aggregated information (list of strings) to the client.
def updateTestPopulation(json_data, conn):
    population = json_data['data']['generation']
    test_names = json_data['data']['tests']
    classname = json_data['data']['classname']
    test_path = json_data['data']['testSuitePath']
    scaffolding_path = json_data['data']['testScaffoldingPath']

    assert(exists(test_path))
    assert (exists(scaffolding_path))

    # Here, the actual orchestrator would perform the patch validation, and then
    # notify the client that validation results can be queried. For testing purposes,
    # we simply aggregate the data and send it back to the client.
    reply_data = [population]
    reply_data.extend(test_names)
    reply_data.append(classname)

    # Serialize data into file
    with open('ServerFile0.json', 'w') as f:
        json.dump(reply_data, f)
        jsonFilePath = f.name

    reply = {
            "cmd": "updateTestPopulation",
            "data": {
                "path": jsonFilePath
            }
    }

    conn.sendall(bytes(json.dumps(reply), 'UTF-8'))

# Sends back dummy patch validation result for one (test, patchId) tuple.
def getPatchValidationResult(json_data, conn):
    test_name = json_data['data']['testId']
    patch_id = json_data['data']['patchId']

    if test_name == 'test1' and int(patch_id) == 7:
        validation_result = True
    else:
        validation_result = False

    reply = {
            "cmd": "getPatchValidationResult",
            "data": {
                "testId": test_name,
                "patchId": patch_id,
                "result": validation_result
                }
            }
    conn.sendall(bytes(json.dumps(reply), 'UTF-8'))

if __name__ == "__main__":
    start_server()