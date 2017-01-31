import json
from itertools import chain

def opt_items(x):
    name, value = x
    if value:
        return [(name, value)]
    else:
        return []

def dict_opt(d, rest):
    lst = map(opt_items, rest.items())
    xs = list(chain.from_iterable(lst))
    return dict(d.items() + xs)

def root(windows, keys = [], init_send = []): 
    return { "main": windows, "keys": keys, "init-send": init_send }

def window(content, title = "", size = None, keys = []):    
    return dict_opt({ "content": content, "keys": keys, "title": title }, {"size", size})

def ui(sym, id = None, send = None):
    return dict_opt(sym, {"id": id, "send": send})

def keys(xs):
    return { "keys": xs }

def key(name, msgs):
    return { "key": msgs }

def msg(client, path, args):
    return { "client": client, "path": path, "args": args }

def ref(n):
    return "$" + str(n)

def send(default = [], cases = [], cases_off = []):
    def make_case(x):
        return ("case " + x[0], x[1])

    def make_case_off(x):
        return ("case-off " + x[0], x[1])      

    return dict([("default", default)] + map(make_case, cases) + map(make_case_off, cases))

# layout widgets

def hor(items):
    return { "hor": items }

def ver(items):
    return { "ver": items }

def space(size):
    return { "space": size }

def tabs(pages):
    return { "tabs": pages }

def page(content, title = ""):
    return { "page": { "content": content, "title": title }}

# valuators

def from_pair_range(range):
    if range:
        return [range[0], range[1]]
    else:
        return None    

def dial(init = None, color = None, range = None):
    return { "dial": dict_opt({}, { "init": init, "color": color, "range": from_pair_range(range) })}

def hfader(init = None, color = None, range = None):
    return { "hfader": dict_opt({}, { "init": init, "color": color, "range": from_pair_range(range) })}

def vfader(init = None, color = None, range = None):
    return { "vfader": dict_opt({}, { "init": init, "color": color, "range": from_pair_range(range) })}

def toggle(init = None, color: None, text = None):
    return { "toggle": dict_opt({}, { "init": init, "color": color, "text": text })}

def button(color: None, text = None):
    return { "button": dict_opt({}, { "color": color, "text": text })}

def circle_toggle(init = None, color: None):
    return { "circle-toggle": dict_opt({}, { "init": init, "color": color, "text": text })}

def circle_button(color: None):
    return { "circle-button": dict_opt({}, { "color": color, "text": text })}

def int_dial(init = None, color = None, range = None):
    return { "int-dial": dict_opt({}, { "init": init, "color": color, "range": from_pair_range(range) })}

def label(color = None, text = None):
    return { "label": dict_opt({}, { "color": color, "text": text })}

def hcheck(init = None, color = None, size = None, texts = None, allow_deselect = None):
    return { "hcheck": dict_opt({}, { "init": init, "color": color, "size": size, "texts": texts, "allow-deselect": allow_deselect })}

def vcheck(init = None, color = None, size = None, texts = None, allow_deselect = None):
    return { "vcheck": dict_opt({}, { "init": init, "color": color, "size": size, "texts": texts, "allow-deselect": allow_deselect })}
