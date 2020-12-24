"""
author: Daryl.Xu
e-mail: xuziqiang@zyheal.com
"""
import os

from pdf_client_wrapper import utils


def test_zip_dir():
    task_dir = '/home/ziqiang_xu/zy/passer-workers/liver-worker/test-data/task1'
    sources_dir = os.path.join(task_dir, 'report/resources')
    target_file = os.path.join(task_dir, 'report/resources.zip')
    utils.zip_dir(sources_dir, target_file)
