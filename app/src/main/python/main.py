import zipfile, json, io

def EditOne(indata):
    with zipfile.ZipFile(io.BytesIO(indata), 'r') as zf:
        if 'MetaData' not in zf.namelist():
            raise FileNotFoundError("No Metadata.It's a broken mod.")
        raw = zf.read('MetaData')

    try:
        data = json.loads(raw)
        data["WorkshopMetadata"] = {}
        edited = json.dumps(data)
    except:
        raise IOError("No WorkshopMetadata.It's a broken mod.")

    out_buf = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(indata), 'r') as zin:
        with zipfile.ZipFile(out_buf, 'w', zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                if item.filename == 'MetaData':
                    zout.writestr(item, edited)
                else:
                    zout.writestr(item, zin.read(item.filename))
    return out_buf.getvalue()

def help():
    print("甜瓜模组联机适配器 | Melmod Multiplayer Patcher")
    print("MMP | By LanGrass")
    print()
    print("语法：mmp [参数] [文件]")
    print()
    print("用法：")
    print("[参数] -s 适配单个文件 [文件] 文件路径/名称")
    print("[参数] -g 适配一个文件夹中的所有文件 [文件] 文件夹路径/名称")
    print("[参数] 无效参数 该帮助")
    print()
    print(f"实例：mmp -s langrass.melmod")
    print(f"\n输入蓝草参数有惊喜")

def lan():
    print("""\033[34m████████████████████████████████████████
████████████████████████████████████████
██                  ████████      ██████
\033[94m██                                    ██
██                                    ██
██                                    ██
██   ████                     ████    ██
██   ████                     ████    ██
██                                    ██
██                                    ██
██     ██                    ██       ██
██       ████████████████████         ██
██                                    ██
██                                    ██
██                                    ██
██                                    ██
████████████████████████████████████████\033[0m""")