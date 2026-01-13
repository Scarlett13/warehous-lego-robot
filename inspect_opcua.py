
import asyncio
from asyncua import Client

async def main():
    url = "opc.tcp://localhost:4840"
    try:
        async with Client(url=url) as client:
            print(f"Connected to {url}")
            namespace_idx = await client.get_namespace_index("urn:warehouse:mas")
            print(f"Namespace Index for 'urn:warehouse:mas': {namespace_idx}")
            
            objects = client.nodes.objects
            print("Browsing Objects folder:")
            children = await objects.get_children()
            for child in children:
                browse_name = await child.read_browse_name()
                print(f" - {browse_name.Name} (NodeId: {child.nodeid})")
                
                # If it looks like a robot folder (ns matches)
                if child.nodeid.NamespaceIndex == namespace_idx:
                    print(f"   Browsing {browse_name.Name}...")
                    vars = await child.get_children()
                    for v in vars:
                        bn = await v.read_browse_name()
                        val = await v.read_value()
                        print(f"     -> {bn.Name}: {val}")
                        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    asyncio.run(main())
