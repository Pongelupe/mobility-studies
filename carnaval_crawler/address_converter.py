#!/usr/bin/env python3
import psycopg2

SELECT_ADDRESS = "select ca.id_address, ca.desc_address from carnival_address ca"
