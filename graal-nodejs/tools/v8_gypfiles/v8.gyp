# Copyright 2012 the V8 project authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
{
  'variables': {
    'V8_ROOT': '../../deps/v8',
    'v8_code': 1,
    'v8_random_seed%': 314159265,
    'v8_vector_stores%': 0,
    'v8_embed_script%': "",
    'mksnapshot_exec': '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)mksnapshot<(EXECUTABLE_SUFFIX)',
    'v8_os_page_size%': 0,
    'generate_bytecode_output_root': '<(SHARED_INTERMEDIATE_DIR)/generate-bytecode-output-root',
    'generate_bytecode_builtins_list_output': '<(generate_bytecode_output_root)/builtins-generated/bytecodes-builtins-list.h',
    'torque_files': ['<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "torque_files = ")'],
    # Torque and V8 expect the files to be named relative to V8's root.
    'torque_files_without_v8_root': ['<!@pymod_do_main(ForEachReplace "<@(V8_ROOT)/" "" <@(torque_files))'],
    'torque_files_replaced': ['<!@pymod_do_main(ForEachReplace ".tq" "-tq" <@(torque_files_without_v8_root))'],
    'torque_outputs_csa_cc': ['<!@pymod_do_main(ForEachFormat "<(SHARED_INTERMEDIATE_DIR)/torque-generated/%s-csa.cc" <@(torque_files_replaced))'],
    'torque_outputs_csa_h': ['<!@pymod_do_main(ForEachFormat "<(SHARED_INTERMEDIATE_DIR)/torque-generated/%s-csa.h" <@(torque_files_replaced))'],
    'torque_outputs_inl_inc': ['<!@pymod_do_main(ForEachFormat "<(SHARED_INTERMEDIATE_DIR)/torque-generated/%s-inl.inc" <@(torque_files_replaced))'],
    'torque_outputs_cc': ['<!@pymod_do_main(ForEachFormat "<(SHARED_INTERMEDIATE_DIR)/torque-generated/%s.cc" <@(torque_files_replaced))'],
    'torque_outputs_inc': ['<!@pymod_do_main(ForEachFormat "<(SHARED_INTERMEDIATE_DIR)/torque-generated/%s.inc" <@(torque_files_replaced))'],
    'conditions': [
      ['v8_enable_i18n_support==1', {
        'torque_files': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "torque_files =.*?v8_enable_i18n_support.*?torque_files \\+= ")',
        ],
      }],
      ['v8_enable_webassembly==1', {
        'torque_files': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "torque_files =.*?v8_enable_webassembly.*?torque_files \\+= ")',
        ],
      }],
    ],
  },
  'includes': ['toolchain.gypi', 'features.gypi'],
  'target_defaults': {
    'msvs_settings': {
      'VCCLCompilerTool': {
        'AdditionalOptions': ['/utf-8']
      }
    },
    'conditions': [
      ['OS=="mac"', {
        # Hide symbols that are not explicitly exported with V8_EXPORT.
        # TODO(joyeecheung): enable it on other platforms. Currently gcc times out
        # or run out of memory with -fvisibility=hidden on some machines in the CI.
        'xcode_settings': {
          'GCC_SYMBOLS_PRIVATE_EXTERN': 'YES',  # -fvisibility=hidden
        },
        'defines': [
          'BUILDING_V8_SHARED',  # Make V8_EXPORT visible.
        ],
      }],
    ],
  },
  'targets': [
    {
      'target_name': 'v8_pch',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'conditions': [
        ['OS == "win" and (clang != 1 or use_ccache_win != 1)', {
          'direct_dependent_settings': {
            'msvs_precompiled_header': '<(V8_ROOT)/../../tools/msvs/pch/v8_pch.h',
            'msvs_precompiled_source': '<(V8_ROOT)/../../tools/msvs/pch/v8_pch.cc',
            'sources': [
              '<(_msvs_precompiled_header)',
              '<(_msvs_precompiled_source)',
            ],
          },
        }],
      ],
    },  # v8_pch
    {
      'target_name': 'run_torque',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'conditions': [
        ['want_separate_host_toolset', {
          'dependencies': ['torque#host'],
        }, {
          'dependencies': ['torque#target'],
        }],
      ],
      'hard_dependency': 1,
      'direct_dependent_settings': {
        'include_dirs': [
          '<(SHARED_INTERMEDIATE_DIR)',
        ],
      },
      'actions': [
        {
          'action_name': 'run_torque_action',
          'inputs': [  # Order matters.
            '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)torque<(EXECUTABLE_SUFFIX)',
            '<@(torque_files)',
          ],
          'outputs': [
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/bit-fields.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtin-definitions.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-debug-readers.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-debug-readers.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-forward-declarations.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-verifiers.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-verifiers.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/csa-types.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/debug-macros.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/debug-macros.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/enum-verifiers.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/exported-macros-assembler.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/exported-macros-assembler.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/factory.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/factory.inc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/instance-types.h",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/interface-descriptors.inc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/objects-body-descriptors-inl.inc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/objects-printer.cc",
            "<(SHARED_INTERMEDIATE_DIR)/torque-generated/visitor-lists.h",
            '<@(torque_outputs_csa_cc)',
            '<@(torque_outputs_csa_h)',
            '<@(torque_outputs_inl_inc)',
            '<@(torque_outputs_cc)',
            '<@(torque_outputs_inc)',
          ],
          'action': [
            '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)torque<(EXECUTABLE_SUFFIX)',
            '-o', '<(SHARED_INTERMEDIATE_DIR)/torque-generated',
            '-v8-root', '<(V8_ROOT)',
            '<@(torque_files_without_v8_root)',
          ],
        },
      ],
    },  # run_torque
    {
      'target_name': 'v8_maybe_icu',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'hard_dependency': 1,
      'conditions': [
        ['v8_enable_i18n_support==1', {
          'dependencies': [
            '<(icu_gyp_path):icui18n',
            '<(icu_gyp_path):icuuc',
          ],
          'export_dependent_settings': [
            '<(icu_gyp_path):icui18n',
            '<(icu_gyp_path):icuuc',
          ],
        }],
      ],
    },  # v8_maybe_icu
    {
      'target_name': 'torque_runtime_support',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<(V8_ROOT)/src/torque/runtime-support.h',
        ],
      },
    },  # torque_runtime_support
    {
      'target_name': 'torque_generated_initializers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'hard_dependency': 1,
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_base_without_compiler',
        'torque_runtime_support',
        'v8_maybe_icu',
      ],
      'direct_dependent_settings': {
        'sources': [
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/csa-types.h',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/enum-verifiers.cc',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/exported-macros-assembler.cc',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/exported-macros-assembler.h',
          '<@(torque_outputs_csa_cc)',
          '<@(torque_outputs_csa_h)',
        ],
      }
    },  # torque_generated_initializers
    {
      'target_name': 'torque_generated_definitions',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'hard_dependency': 1,
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_internal_headers',
        'v8_libbase',
        'v8_maybe_icu',
      ],
      'direct_dependent_settings': {
        'sources': [
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-forward-declarations.h',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-verifiers.cc',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/class-verifiers.h',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/factory.cc',
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/objects-printer.cc',
          '<@(torque_outputs_inl_inc)',
          '<@(torque_outputs_cc)',
          '<@(torque_outputs_inc)',
        ],
        'include_dirs': [
          '<(SHARED_INTERMEDIATE_DIR)',
        ],
      },
    },  # torque_generated_definitions
    {
      'target_name': 'generate_bytecode_builtins_list',
      'type': 'none',
      'hard_dependency': 1,
      'toolsets': ['host', 'target'],
      'conditions': [
        ['want_separate_host_toolset', {
          'dependencies': ['bytecode_builtins_list_generator#host'],
        }, {
          'dependencies': ['bytecode_builtins_list_generator#target'],
        }],
      ],
      'direct_dependent_settings': {
        'sources': [
          '<(generate_bytecode_builtins_list_output)',
        ],
        'include_dirs': [
          '<(generate_bytecode_output_root)',
          '<(SHARED_INTERMEDIATE_DIR)',
        ],
      },
      'actions': [
        {
          'action_name': 'generate_bytecode_builtins_list_action',
          'inputs': [
            '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)bytecode_builtins_list_generator<(EXECUTABLE_SUFFIX)',
          ],
          'outputs': [
            '<(generate_bytecode_builtins_list_output)',
          ],
          'action': [
            '<(python)',
            '<(V8_ROOT)/tools/run.py',
            '<@(_inputs)',
            '<@(_outputs)',
          ],
        },
      ],
    },  # generate_bytecode_builtins_list
    {
      'target_name': 'v8_init',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_base_without_compiler',
        'v8_initializers',
        'v8_maybe_icu',
        'abseil.gyp:abseil',
      ],
      'sources': [
        '<(V8_ROOT)/src/init/setup-isolate-full.cc',
      ],
    },  # v8_init
    {
      # This target is used to work around a GCC issue that causes the
      # compilation to take several minutes when using -O2 or -O3.
      # This is fixed in GCC 13.
      'target_name': 'v8_initializers_slow',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'abseil.gyp:abseil',
      ],
      'cflags!': ['-O3'],
      'cflags': ['-O1'],
      'sources': [
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/js-to-wasm-tq-csa.h',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/js-to-wasm-tq-csa.cc',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/wasm-to-js-tq-csa.h',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/wasm-to-js-tq-csa.cc',
      ],
      'conditions': [
        ['v8_enable_i18n_support==1', {
          'dependencies': [
            '<(icu_gyp_path):icui18n',
            '<(icu_gyp_path):icuuc',
          ],
        }],
      ],
    },  # v8_initializers_slow
    {
      'target_name': 'v8_initializers',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'torque_generated_initializers',
        'v8_base_without_compiler',
        'v8_shared_internal_headers',
        'v8_pch',
        'abseil.gyp:abseil',
      ],
      'include_dirs': [
        '<(SHARED_INTERMEDIATE_DIR)',
        '<(generate_bytecode_output_root)',
      ],
      'sources': [
        '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "\\"v8_initializers.*?sources = ")',
      ],
      'conditions': [
        ['v8_enable_webassembly==1', {
          'dependencies': [
            'v8_initializers_slow',
          ],
          # Compiled by v8_initializers_slow target.
          'sources!': [
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/js-to-wasm-tq-csa.h',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/js-to-wasm-tq-csa.cc',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/wasm-to-js-tq-csa.h',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/src/builtins/wasm-to-js-tq-csa.cc',
          ],
          'sources': [
            '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "\\"v8_initializers.*?v8_enable_webassembly.*?sources \\+= ")',
          ],
        }],
        ['v8_target_arch=="ia32"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/ia32/builtins-ia32.cc',
          ],
        }],
        ['v8_target_arch=="x64"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/x64/builtins-x64.cc',
          ],
        }],
        ['v8_target_arch=="arm"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/arm/builtins-arm.cc',
          ],
        }],
        ['v8_target_arch=="arm64"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/arm64/builtins-arm64.cc',
          ],
        }],
        ['v8_target_arch=="riscv64" or v8_target_arch=="riscv64"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/riscv/builtins-riscv.cc',
          ],
        }],
        ['v8_target_arch=="loong64" or v8_target_arch=="loong64"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/loong64/builtins-loong64.cc',
          ],
        }],
        ['v8_target_arch=="mips64" or v8_target_arch=="mips64el"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/mips64/builtins-mips64.cc',
          ],
        }],
        ['v8_target_arch=="ppc"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/ppc/builtins-ppc.cc',
          ],
        }],
        ['v8_target_arch=="ppc64"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/ppc/builtins-ppc.cc',
          ],
        }],
        ['v8_target_arch=="s390x"', {
          'sources': [
            '<(V8_ROOT)/src/builtins/s390/builtins-s390.cc',
          ],
        }],
        ['v8_enable_i18n_support==1', {
          'dependencies': [
            '<(icu_gyp_path):icui18n',
            '<(icu_gyp_path):icuuc',
          ],
        }, {
           'sources!': [
             '<(V8_ROOT)/src/builtins/builtins-intl-gen.cc',
           ],
         }],
      ],
    },  # v8_initializers
    {
      'target_name': 'v8_snapshot',
      'type': 'static_library',
      'toolsets': ['target'],
      'sources': [
        '<(V8_ROOT)/src/init/setup-isolate-deserialize.cc',
      ],
      'xcode_settings': {
        # V8 7.4 over macOS10.11 compatibility
        # Refs: https://github.com/nodejs/node/pull/26685
        'GCC_GENERATE_DEBUGGING_SYMBOLS': 'NO',
      },
      'actions': [],
      'conditions': [
        ['want_separate_host_toolset', {
          'dependencies': [
            'v8_base_without_compiler',
          ]
        }, {
          'dependencies': [
            'v8_base_without_compiler',
          ]
        }],
        ['OS=="win" and clang==1', {
          'actions': [
            {
              'action_name': 'asm_to_inline_asm',
              'message': 'generating: >@(_outputs)',
              'inputs': [
                '<(INTERMEDIATE_DIR)/embedded.S',
              ],
              'outputs': [
                '<(INTERMEDIATE_DIR)/embedded.cc',
              ],
              'process_outputs_as_sources': 1,
              'action': [
                '<(python)',
                '<(V8_ROOT)/tools/snapshot/asm_to_inline_asm.py',
                '<@(_inputs)',
                '<(INTERMEDIATE_DIR)/embedded.cc',
              ],
            },
          ],
        }],
      ],
    },  # v8_snapshot
    {
      'target_name': 'v8_version',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<(V8_ROOT)/include/v8-value-serializer-version.h',
          '<(V8_ROOT)/include/v8-version-string.h',
          '<(V8_ROOT)/include/v8-version.h',
        ],
      },
    },  # v8_version
    {
      'target_name': 'v8_config_headers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<(V8_ROOT)/include/v8-platform.h',
          '<(V8_ROOT)/include/v8-source-location.h',
          '<(V8_ROOT)/include/v8config.h',
        ],
      },
    },  # v8_config_headers
    {
      'target_name': 'v8_headers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_config_headers',
        'v8_heap_base_headers',
        'v8_version',
      ],
      'direct_dependent_settings': {
        'sources': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_headers\\".*?sources = ")',

          '<(V8_ROOT)/include/v8-wasm-trap-handler-posix.h',
          '<(V8_ROOT)/include/v8-wasm-trap-handler-win.h',
        ],
      },
    },  # v8_headers
    {
      'target_name': 'v8_shared_internal_headers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_headers',
        'v8_libbase',
      ],
      'direct_dependent_settings': {
        'sources': [
          '<(V8_ROOT)/src/common/globals.h',
          '<(V8_ROOT)/src/wasm/wasm-constants.h',
          '<(V8_ROOT)/src/wasm/wasm-limits.h',
        ],
      },
    },  # v8_shared_internal_headers
    {
      'target_name': 'v8_flags',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_libbase',
        'v8_shared_internal_headers',
      ],
      'direct_dependent_settings': {
        'sources': [
          '<(V8_ROOT)/src/flags/flag-definitions.h',
          '<(V8_ROOT)/src/flags/flags-impl.h',
          '<(V8_ROOT)/src/flags/flags.h',
        ],
      },
    },  # v8_flags
    {
      'target_name': 'v8_internal_headers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'torque_runtime_support',
        'v8_flags',
        'v8_headers',
        'v8_maybe_icu',
        'v8_shared_internal_headers',
        'v8_heap_base_headers',
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_libbase',
        'fp16',
        'abseil.gyp:abseil',
      ],
      'direct_dependent_settings': {
        'sources': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?sources = ")',
        ],
        'conditions': [
          ['v8_enable_snapshot_compression==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_snapshot_compression.*?sources \\+= ")',
            ],
          }],
          ['v8_enable_sparkplug==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_sparkplug.*?sources \\+= ")',
            ],
          }],
          ['v8_enable_maglev==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_maglev.*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_target_arch=="arm"', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_maglev.*?v8_current_cpu == \\"arm\\".*?sources \\+= ")',
                ],
              }],
              ['v8_target_arch=="arm64"', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_maglev.*?v8_current_cpu == \\"arm64\\".*?sources \\+= ")',
                ],
              }],
              ['v8_target_arch=="x64"', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_maglev.*?v8_current_cpu == \\"x64\\".*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_enable_webassembly==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_webassembly.*?sources \\+= ")',
            ],
          }],
          ['v8_enable_wasm_simd256_revec==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_wasm_simd256_revec.*?sources \\+= ")',
            ],
          }],
          ['v8_enable_i18n_support==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?sources \\+= ")',
            ],
          }],
          ['v8_control_flow_integrity==0', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?!v8_control_flow_integrity.*?sources \\+= ")',
            ],
          }],
          ['v8_enable_heap_snapshot_verify==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_heap_snapshot_verify.*?sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="ia32"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"x86\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"x86\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="x64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"x64\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['OS=="win"', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"x64\\".*?is_win.*?sources \\+= ")',
                ],
              }],
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"x64\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
              ['v8_enable_webassembly==1', {
                'conditions': [
                  ['OS in "linux mac ios freebsd openharmony"', {
                    'sources': [
                      '<(V8_ROOT)/src/trap-handler/handler-inside-posix.h',
                    ],
                  }],
                  ['OS=="win"', {
                    'sources': [
                      '<(V8_ROOT)/src/trap-handler/handler-inside-win.h',
                    ],
                  }],
                ],
              }],
            ],
          }],
          ['v8_target_arch=="arm"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"arm\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"arm\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="arm64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"arm64\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_control_flow_integrity==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"arm64\\".*?v8_control_flow_integrity.*?sources \\+= ")',
                ],
              }],
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"arm64\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
              ['v8_enable_webassembly==1', {
                'conditions': [
                  ['((_toolset=="host" and host_arch=="arm64" or _toolset=="target" and target_arch=="arm64") and (OS in "linux mac openharmony")) or ((_toolset=="host" and host_arch=="x64" or _toolset=="target" and target_arch=="x64") and (OS in "linux mac openharmony"))', {
                    'sources': [
                      '<(V8_ROOT)/src/trap-handler/handler-inside-posix.h',
                    ],
                  }],
                  ['(_toolset=="host" and host_arch=="x64" or _toolset=="target" and target_arch=="x64") and (OS in "linux mac win openharmony")', {
                    'sources': [
                      '<(V8_ROOT)/src/trap-handler/trap-handler-simulator.h',
                    ],
                  }],
                ],
              }],
              ['OS=="win"', {
                'sources': [
                  '<(V8_ROOT)/src/diagnostics/unwinding-info-win64.h',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="mips64" or v8_target_arch=="mips64el"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"mips64\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"mips64\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="ppc"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"ppc\\".*?sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="ppc64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"ppc64\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"ppc64\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="s390x"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"s390\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"s390\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="riscv64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"riscv64\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"riscv64\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
          ['v8_target_arch=="loong64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"loong64\\".*?sources \\+= ")',
            ],
            'conditions': [
              ['v8_enable_sparkplug==1', {
                'sources': [
                  '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_internal_headers\\".*?v8_enable_i18n_support.*?v8_current_cpu == \\"loong64\\".*?v8_enable_sparkplug.*?sources \\+= ")',
                ],
              }],
            ],
          }],
        ],
      },
    },  # v8_internal_headers
    {
      'target_name': 'v8_compiler_sources',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': ['<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources = ")'],
        'conditions': [
          ['v8_target_arch=="ia32"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"x86\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="x64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"x64\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="arm"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"arm\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="arm64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"arm64\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="mips64" or v8_target_arch=="mips64el"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"mips64\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="ppc"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"ppc\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="ppc64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"ppc64\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="s390x"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"s390x\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="riscv64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"riscv64\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_target_arch=="loong64"', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_current_cpu == \\"loong64\\".*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_enable_webassembly==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_enable_webassembly.*?v8_compiler_sources \\+= ")',
            ],
          }],
          ['v8_enable_wasm_simd256_revec==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_compiler_sources =.*?v8_enable_wasm_simd256_revec.*?v8_compiler_sources \\+= ")',
            ],
          }],
        ],
      }
    },  # v8_compiler_sources
    {
      'target_name': 'v8_compiler_for_mksnapshot_source_set',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_maybe_icu',
        'v8_base_without_compiler',
        'v8_internal_headers',
        'v8_libbase',
        'v8_shared_internal_headers',
        'v8_pch',
      ],
      'conditions': [
        ['v8_enable_turbofan==1', {
          'dependencies': ['v8_compiler_sources'],
        }, {
          'sources': ['<(V8_ROOT)/src/compiler/turbofan-disabled.cc'],
        }],
      ],
    },  # v8_compiler_for_mksnapshot_source_set
    {
      'target_name': 'v8_compiler',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_internal_headers',
        'v8_maybe_icu',
        'v8_base_without_compiler',
        'v8_libbase',
        'v8_shared_internal_headers',
        'v8_turboshaft',
        'v8_pch',
        'abseil.gyp:abseil',
      ],
      'conditions': [
        ['v8_enable_turbofan==1', {
          'dependencies': ['v8_compiler_sources'],
        }, {
          'sources': ['<(V8_ROOT)/src/compiler/turbofan-disabled.cc'],
        }],
      ],
    },  # v8_compiler
    {
      'target_name': 'v8_turboshaft',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_internal_headers',
        'v8_maybe_icu',
        'v8_base_without_compiler',
        'v8_libbase',
        'v8_shared_internal_headers',
        'v8_pch',
        'abseil.gyp:abseil',
      ],
      'sources': [
        '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_source_set.\\"v8_turboshaft.*?sources = ")',
      ],
      'conditions': [
        ['v8_enable_maglev==0', {
          'sources': [
            '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_source_set.\\"v8_turboshaft.*?!v8_enable_maglev.*?sources \\+= ")',
          ],
        }],
      ],
      'msvs_settings': {
        'VCCLCompilerTool': {
          'AdditionalOptions': [
            '/bigobj'
          ],
        },
      },
    },  # v8_turboshaft
    {
      'target_name': 'v8_compiler_for_mksnapshot',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'hard_dependency': 1,
      'dependencies': [
        'generate_bytecode_builtins_list',
        'run_torque',
        'v8_maybe_icu',
      ],
      'conditions': [
        ['(is_component_build and not v8_optimized_debug and v8_enable_fast_mksnapshot) or v8_enable_turbofan==0', {
          'dependencies': [
            'v8_compiler_for_mksnapshot_source_set',
          ],
          'export_dependent_settings': [
            'v8_compiler_for_mksnapshot_source_set',
          ],
        }, {
           'dependencies': [
             'v8_compiler',
           ],
           'export_dependent_settings': [
             'v8_compiler',
           ],
         }],
      ],
    },  # v8_compiler_for_mksnapshot
    {
      'target_name': 'v8_inspector_headers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'hard_dependency': 1,
      'includes': ['inspector.gypi'],
      'direct_dependent_settings': {
        'include_dirs': [
          '<(inspector_generated_output_root)/include',
        ],
      },
      'actions': [
        {
          'action_name': 'protocol_compatibility',
          'inputs': [
            '<(v8_inspector_js_protocol)',
          ],
          'outputs': [
            '<@(inspector_generated_output_root)/src/js_protocol.stamp',
          ],
          'action': [
            '<(python)',
            '<(inspector_protocol_path)/check_protocol_compatibility.py',
            '--stamp', '<@(_outputs)',
            '<@(_inputs)',
          ],
          'message': 'Checking inspector protocol compatibility',
        },
        {
          'action_name': 'protocol_generated_sources',
          'inputs': [
            '<(v8_inspector_js_protocol)',
            '<(inspector_path)/inspector_protocol_config.json',
            '<@(inspector_protocol_files)',
          ],
          'outputs': [
            '<@(inspector_generated_sources)',
          ],
          'process_outputs_as_sources': 1,
          'action': [
            '<(python)',
            '<(inspector_protocol_path)/code_generator.py',
            '--jinja_dir', '<(V8_ROOT)/third_party',
            '--output_base', '<(inspector_generated_output_root)/src/inspector',
            '--config', '<(inspector_path)/inspector_protocol_config.json',
            '--config_value', 'protocol.path=<(v8_inspector_js_protocol)',
            '--inspector_protocol_dir', '<(inspector_protocol_path)',
          ],
          'message': 'Generating inspector protocol sources from protocol json',
        },
      ],
    },  # v8_inspector_headers
    {
      'target_name': 'v8_base_without_compiler',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_libbase',
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '<(SHARED_INTERMEDIATE_DIR)',
        ],
      },
      'variables': {
        'optimize': 'max',
      },
      'include_dirs': [
        '../../deps/uv/include',
        '<(java_home)/include/',
        '<(DEPTH)',
        '<(SHARED_INTERMEDIATE_DIR)'
      ],
      'conditions': [
        ['OS=="linux"', {
          'include_dirs+': [
            '<(java_home)/include/linux/',
          ],
        }],
        ['OS=="win"', {
          'include_dirs+': [
            '<(java_home)/include/win32/',
          ],
        }],
        ['OS=="solaris"', {
          'include_dirs+': [
            '<(java_home)/include/solaris/',
          ],
        }],
        ['OS=="mac"', {
          'include_dirs+': [
            '<(java_home)/include/darwin/',
          ],
        }],
        ['OS != "win"', {
          'defines': [ '__POSIX__' ],
        }],
      ],
      'link_settings': {
        'conditions' : [
          ['OS=="linux" or OS=="solaris"', {
            'libraries': [
              '-L<(java_home)/lib',
              # standalone (./bin/node)
              "-Wl,-rpath='$$ORIGIN/../lib/'",
              # graalvm (./languages/nodejs/bin/node)
              "-Wl,-rpath='$$ORIGIN/../../../lib/'",
            ],
          }],
          ['OS=="mac"', {
            'libraries': [
              '-L<(java_home)/lib',
              # standalone (./bin/node)
              "-Wl,-rpath,'@loader_path/../lib/'",
              # graalvm (./languages/nodejs/bin/node)
              "-Wl,-rpath,'@loader_path/../../../lib/'",
            ],
          }],
          ['OS == "win"', {
            'libraries': [
              '-lDbghelp',
            ],
          }],
          ['OS != "win"', {
            'libraries': [
              '-ljsig',
              '-ldl',
            ],
          }],
       ]},
      'sources': [
        '<(V8_ROOT)/include/v8-platform.h',
        '<(V8_ROOT)/include/v8-profiler.h',
        '<(V8_ROOT)/include/v8-testing.h',
        '<(V8_ROOT)/include/v8-util.h',
        '<(V8_ROOT)/include/v8-value-serializer-version.h',
        '<(V8_ROOT)/include/v8-version-string.h',
        '<(V8_ROOT)/include/v8-version.h',
        '<(V8_ROOT)/include/v8.h',
        '<(V8_ROOT)/include/v8config.h',
        '<(V8_ROOT)/src/graal/callbacks.cc',
        '<(V8_ROOT)/src/graal/graal_array.cc',
        '<(V8_ROOT)/src/graal/graal_array_buffer.cc',
        '<(V8_ROOT)/src/graal/graal_array_buffer_view.cc',
        '<(V8_ROOT)/src/graal/graal_big_int.cc',
        '<(V8_ROOT)/src/graal/graal_boolean.cc',
        '<(V8_ROOT)/src/graal/graal_context.cc',
        '<(V8_ROOT)/src/graal/graal_date.cc',
        '<(V8_ROOT)/src/graal/graal_external.cc',
        '<(V8_ROOT)/src/graal/graal_fixed_array.cc',
        '<(V8_ROOT)/src/graal/graal_function.cc',
        '<(V8_ROOT)/src/graal/graal_function_template.cc',
        '<(V8_ROOT)/src/graal/graal_handle_content.cc',
        '<(V8_ROOT)/src/graal/graal_isolate.cc',
        '<(V8_ROOT)/src/graal/graal_map.cc',
        '<(V8_ROOT)/src/graal/graal_message.cc',
        '<(V8_ROOT)/src/graal/graal_missing_primitive.cc',
        '<(V8_ROOT)/src/graal/graal_module.cc',
        '<(V8_ROOT)/src/graal/graal_module_request.cc',
        '<(V8_ROOT)/src/graal/graal_number.cc',
        '<(V8_ROOT)/src/graal/graal_object.cc',
        '<(V8_ROOT)/src/graal/graal_object_template.cc',
        '<(V8_ROOT)/src/graal/graal_primitive_array.cc',
        '<(V8_ROOT)/src/graal/graal_promise.cc',
        '<(V8_ROOT)/src/graal/graal_proxy.cc',
        '<(V8_ROOT)/src/graal/graal_regexp.cc',
        '<(V8_ROOT)/src/graal/graal_script.cc',
        '<(V8_ROOT)/src/graal/graal_script_or_module.cc',
        '<(V8_ROOT)/src/graal/graal_set.cc',
        '<(V8_ROOT)/src/graal/graal_stack_frame.cc',
        '<(V8_ROOT)/src/graal/graal_stack_trace.cc',
        '<(V8_ROOT)/src/graal/graal_string.cc',
        '<(V8_ROOT)/src/graal/graal_symbol.cc',
        '<(V8_ROOT)/src/graal/graal_template.cc',
        '<(V8_ROOT)/src/graal/graal_unbound_script.cc',
        '<(V8_ROOT)/src/graal/graal_value.cc',
        '<(V8_ROOT)/src/graal/graal_wasm_streaming.cc',
        '<(V8_ROOT)/src/graal/microtask_queue.cc',
        '<(V8_ROOT)/src/graal/v8.cc'
      ],
    },  # v8_base_without_compiler
    {
      'target_name': 'v8_base',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_base_without_compiler',
      ],
      'conditions': [
        ['v8_enable_turbofan==1', {
          'dependencies': [
            'v8_turboshaft',
          ],
        }],
      ],
    },  # v8_base
    {
      'target_name': 'torque_base',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'sources': [
        '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "\\"torque_base.*?sources = ")',
      ],
      'dependencies': [
        'v8_shared_internal_headers',
        'v8_libbase',
      ],
      'defines!': [
        '_HAS_EXCEPTIONS=0',
        'BUILDING_V8_SHARED=1',
      ],
      'cflags_cc!': ['-fno-exceptions'],
      'cflags_cc': ['-fexceptions'],
      'xcode_settings': {
        'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',  # -fexceptions
      },
      'msvs_settings': {
        'VCCLCompilerTool': {
          'RuntimeTypeInfo': 'true',
          'ExceptionHandling': 1,
        },
      },
    },  # torque_base
    {
      'target_name': 'torque_ls_base',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'sources': [
        '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "\\"torque_ls_base.*?sources = ")',
      ],
      'dependencies': [
        'torque_base',
      ],
      'defines!': [
        '_HAS_EXCEPTIONS=0',
        'BUILDING_V8_SHARED=1',
      ],
      'cflags_cc!': ['-fno-exceptions'],
      'cflags_cc': ['-fexceptions'],
      'xcode_settings': {
        'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',  # -fexceptions
      },
      'msvs_settings': {
        'VCCLCompilerTool': {
          'RuntimeTypeInfo': 'true',
          'ExceptionHandling': 1,
        },
      },
    },  # torque_ls_base
    {
      'target_name': 'v8_libbase',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'variables': {
        'optimize': 'max',
      },
      'include_dirs': [
        '..',
      ],
      'direct_dependent_settings': {
        'include_dirs': ['..'],
      },
      'sources': [
      ],
    },  # v8_libbase
    {
      'target_name': 'v8_libplatform',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_libbase',
      ],
      'sources': [
        '<(V8_ROOT)/base/trace_event/common/trace_event_common.h',
        '<(V8_ROOT)/include/libplatform/libplatform-export.h',
        '<(V8_ROOT)/include/libplatform/libplatform.h',
        '<(V8_ROOT)/include/libplatform/v8-tracing.h',
        '<(V8_ROOT)/src/libplatform/default-foreground-task-runner.cc',
        '<(V8_ROOT)/src/libplatform/default-foreground-task-runner.h',
        '<(V8_ROOT)/src/libplatform/default-job.cc',
        '<(V8_ROOT)/src/libplatform/default-job.h',
        '<(V8_ROOT)/src/libplatform/default-platform.cc',
        '<(V8_ROOT)/src/libplatform/default-platform.h',
        '<(V8_ROOT)/src/libplatform/default-thread-isolated-allocator.cc',
        '<(V8_ROOT)/src/libplatform/default-thread-isolated-allocator.h',
        '<(V8_ROOT)/src/libplatform/default-worker-threads-task-runner.cc',
        '<(V8_ROOT)/src/libplatform/default-worker-threads-task-runner.h',
        '<(V8_ROOT)/src/libplatform/delayed-task-queue.cc',
        '<(V8_ROOT)/src/libplatform/delayed-task-queue.h',
        '<(V8_ROOT)/src/libplatform/task-queue.cc',
        '<(V8_ROOT)/src/libplatform/task-queue.h',
        '<(V8_ROOT)/src/libplatform/tracing/trace-buffer.cc',
        '<(V8_ROOT)/src/libplatform/tracing/trace-buffer.h',
        '<(V8_ROOT)/src/libplatform/tracing/trace-config.cc',
        '<(V8_ROOT)/src/libplatform/tracing/trace-object.cc',
        '<(V8_ROOT)/src/libplatform/tracing/trace-writer.cc',
        '<(V8_ROOT)/src/libplatform/tracing/trace-writer.h',
        '<(V8_ROOT)/src/libplatform/tracing/tracing-controller.cc',
        '<(V8_ROOT)/src/libplatform/worker-thread.cc',
        '<(V8_ROOT)/src/libplatform/worker-thread.h',
      ],
      'conditions': [
        ['component=="shared_library"', {
          'direct_dependent_settings': {
            'defines': ['USING_V8_PLATFORM_SHARED'],
          },
          'defines': ['BUILDING_V8_PLATFORM_SHARED'],
        }],
        ['v8_use_perfetto==1', {
          'sources!': [
            '<(V8_ROOT)/base/trace_event/common/trace_event_common.h',
            '<(V8_ROOT)/src/libplatform/tracing/trace-buffer.cc',
            '<(V8_ROOT)/src/libplatform/tracing/trace-buffer.h',
            '<(V8_ROOT)/src/libplatform/tracing/trace-object.cc',
            '<(V8_ROOT)/src/libplatform/tracing/trace-writer.cc',
            '<(V8_ROOT)/src/libplatform/tracing/trace-writer.h',
          ],
          'sources': [
            '<(V8_ROOT)/src/libplatform/tracing/trace-event-listener.h',
          ],
          'dependencies': [
            '<(V8_ROOT)/third_party/perfetto:libperfetto',
            '<(V8_ROOT)/third_party/perfetto/protos/perfetto/trace:lite',
          ],
        }],
        ['v8_enable_system_instrumentation==1 and is_win', {
          'sources': [
            '<(V8_ROOT)/src/libplatform/tracing/recorder.h',
            '<(V8_ROOT)/src/libplatform/tracing/recorder-win.cc',
          ],
        }],
        ['v8_enable_system_instrumentation==1 and OS=="mac"', {
          'sources': [
            '<(V8_ROOT)/src/libplatform/tracing/recorder.h',
            '<(V8_ROOT)/src/libplatform/tracing/recorder-mac.cc',
          ],
        }],
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '<(V8_ROOT)/include',
        ],
      },
    },  # v8_libplatform
    {
      'target_name': 'v8_libsampler',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': [
        'v8_libbase',
      ],
      'sources': [
      ],
    },  # v8_libsampler
    {
      'target_name': 'bytecode_builtins_list_generator',
      'type': 'executable',
      'conditions': [
        ['want_separate_host_toolset', {
          'toolsets': ['host'],
        }],
        # Avoid excessive LTO
        ['enable_lto=="true"', {
          'ldflags': [ '-fno-lto' ],
        }],
      ],
      'defines!': [
        'BUILDING_V8_SHARED=1',
      ],
      'dependencies': [
        "v8_libbase",
        # "build/win:default_exe_manifest",
      ],
      'sources': [
        "<(V8_ROOT)/src/builtins/generate-bytecodes-builtins-list.cc",
        "<(V8_ROOT)/src/interpreter/bytecode-operands.cc",
        "<(V8_ROOT)/src/interpreter/bytecode-operands.h",
        "<(V8_ROOT)/src/interpreter/bytecode-traits.h",
        "<(V8_ROOT)/src/interpreter/bytecodes.cc",
        "<(V8_ROOT)/src/interpreter/bytecodes.h",
      ],
    },  # bytecode_builtins_list_generator
    {
      'target_name': 'mksnapshot',
      'type': 'executable',
      'dependencies': [
        'v8_base_without_compiler',
        'v8_compiler_for_mksnapshot',
        'v8_init',
        'v8_libbase',
        'v8_libplatform',
        'v8_maybe_icu',
        'v8_turboshaft',
        'v8_pch',
        'abseil.gyp:abseil',
        # "build/win:default_exe_manifest",
      ],
      'sources': [
        '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "\\"mksnapshot.*?sources = ")',
      ],
      'conditions': [
        ['want_separate_host_toolset', {
          'toolsets': ['host'],
        }],
        # Avoid excessive LTO
        ['enable_lto=="true"', {
          'ldflags': [ '-fno-lto' ],
        }],
      ],
    },  # mksnapshot
    {
      'target_name': 'torque',
      'type': 'executable',
      'dependencies': [
        'torque_base',
        # "build/win:default_exe_manifest",
      ],
      'conditions': [
        ['want_separate_host_toolset', {
          'toolsets': ['host'],
        }],
        # Avoid excessive LTO
        ['enable_lto=="true"', {
          'ldflags': [ '-fno-lto' ],
        }],
      ],
      'defines!': [
        '_HAS_EXCEPTIONS=0',
        'BUILDING_V8_SHARED=1',
      ],
      'cflags_cc!': ['-fno-exceptions'],
      'cflags_cc': ['-fexceptions'],
      'xcode_settings': {
        'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',  # -fexceptions
      },
      'msvs_settings': {
        'VCCLCompilerTool': {
          'RuntimeTypeInfo': 'true',
          'ExceptionHandling': 1,
        },
        'VCLinkerTool': {
          'AdditionalDependencies': [
            'dbghelp.lib',
            'winmm.lib',
            'ws2_32.lib'
          ]
        }
      },
      'sources': [
        "<(V8_ROOT)/src/torque/torque.cc",
      ],
    },  # torque
    {
      'target_name': 'torque-language-server',
      'type': 'executable',
      'conditions': [
        ['want_separate_host_toolset', {
          'toolsets': ['host'],
        }],
        # Avoid excessive LTO
        ['enable_lto=="true"', {
          'ldflags': [ '-fno-lto' ],
        }],
      ],
      'dependencies': [
        'torque_base',
        'torque_ls_base',
        # "build/win:default_exe_manifest",
      ],
      'defines!': [
        '_HAS_EXCEPTIONS=0',
        'BUILDING_V8_SHARED=1',
      ],
      'msvs_settings': {
        'VCCLCompilerTool': {
          'RuntimeTypeInfo': 'true',
          'ExceptionHandling': 1,
        },
      },
      'sources': [
        "<(V8_ROOT)/src/torque/ls/torque-language-server.cc",
      ],
    },  # torque-language-server
    {
      'target_name': 'gen-regexp-special-case',
      'type': 'executable',
      'dependencies': [
        'v8_libbase',
        # "build/win:default_exe_manifest",
        'v8_maybe_icu',
      ],
      'conditions': [
        ['want_separate_host_toolset', {
          'toolsets': ['host'],
        }],
        # Avoid excessive LTO
        ['enable_lto=="true"', {
          'ldflags': [ '-fno-lto' ],
        }],
      ],
      'sources': [
        "<(V8_ROOT)/src/regexp/gen-regexp-special-case.cc",
        "<(V8_ROOT)/src/regexp/special-case.h",
      ],
    },  # gen-regexp-special-case
    {
      'target_name': 'run_gen-regexp-special-case',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'conditions': [
        ['want_separate_host_toolset', {
          'dependencies': ['gen-regexp-special-case#host'],
        }, {
          'dependencies': ['gen-regexp-special-case#target'],
        }],
        # Avoid excessive LTO
        ['enable_lto=="true"', {
          'ldflags': [ '-fno-lto' ],
        }],
      ],
      'actions': [
        {
          'action_name': 'run_gen-regexp-special-case_action',
          'inputs': [
            '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)gen-regexp-special-case<(EXECUTABLE_SUFFIX)',
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/src/regexp/special-case.cc',
          ],
          'action': [
            '<(python)',
            '<(V8_ROOT)/tools/run.py',
            '<@(_inputs)',
            '<@(_outputs)',
          ],
        },
      ],
    },  # run_gen-regexp-special-case
    {
      'target_name': 'v8_heap_base_headers',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_header_set.\\"v8_heap_base_headers.*?sources = ")',
        ],
      },
    },  # v8_heap_base_headers
    {
      'target_name': 'cppgc_base',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_source_set.\\"cppgc_base.*?sources = ")',
        ],
      },
    },  # cppgc_base
    {
      'target_name': 'v8_bigint',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_source_set.\\"v8_bigint.*?sources = ")',
        ],
        'conditions': [
          ['v8_advanced_bigint_algorithms==1', {
            'sources': [
              '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_source_set.\\"v8_bigint.*?v8_advanced_bigint_algorithms.*?sources \\+= ")',
            ],
          }],
        ],
      },
    },  # v8_bigint
    {
      'target_name': 'v8_heap_base',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'direct_dependent_settings': {
        'sources': [
          '<!@pymod_do_main(GN-scraper "<(V8_ROOT)/BUILD.gn"  "v8_source_set.\\"v8_heap_base.*?sources = ")',
        ],
        'conditions': [
          ['enable_lto=="true"', {
            'cflags_cc': [ '-fno-lto' ],
          }],
          # Changes in push_registers_asm.cc in V8 v12.8 requires using
          # push_registers_masm on Windows even with ClangCL on x64
          ['OS=="win"', {
            'conditions': [
              ['_toolset == "host" and host_arch == "x64" or _toolset == "target" and target_arch=="x64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/x64/push_registers_masm.asm',
                ],
              }],
              ['_toolset == "host" and host_arch == "arm64" or _toolset == "target" and target_arch=="arm64"', {
                'conditions': [
                  ['clang==1', {
                    'sources': [
                      '<(V8_ROOT)/src/heap/base/asm/arm64/push_registers_asm.cc',
                    ],
                  }],
                  ['clang==0', {
                    'sources': [
                      '<(V8_ROOT)/src/heap/base/asm/arm64/push_registers_masm.S',
                    ],
                  }],
                ],
              }],
            ],
          }, { # 'OS!="win"'
            'conditions': [
              ['_toolset == "host" and host_arch == "x64" or _toolset == "target" and target_arch=="x64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/x64/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "ia32" or _toolset == "target" and target_arch=="ia32"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/ia32/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "arm" or _toolset == "target" and target_arch=="arm"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/arm/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "arm64" or _toolset == "target" and target_arch=="arm64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/arm64/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "ppc64" or _toolset == "target" and target_arch=="ppc64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/ppc/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "s390x" or _toolset == "target" and target_arch=="s390x"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/s390/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "mips64" or _toolset == "target" and target_arch=="mips64" or _toolset == "host" and host_arch == "mips64el" or _toolset == "target" and target_arch=="mips64el"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/mips64/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "riscv64" or _toolset == "target" and target_arch=="riscv64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/riscv/push_registers_asm.cc',
                ],
              }],
              ['_toolset == "host" and host_arch == "loong64" or _toolset == "target" and target_arch=="loong64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/loong64/push_registers_asm.cc',
                ],
              }],
            ]
          }],
          ['OS=="win" and clang==0', {
            'conditions': [
              ['_toolset == "host" and host_arch == "x64" or _toolset == "target" and target_arch=="x64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/x64/push_registers_masm.asm',
                ],
              }],
              ['_toolset == "host" and host_arch == "ia32" or _toolset == "target" and target_arch=="ia32"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/ia32/push_registers_masm.asm',
                ],
              }],
              ['_toolset == "host" and host_arch == "arm64" or _toolset == "target" and target_arch=="arm64"', {
                'sources': [
                  '<(V8_ROOT)/src/heap/base/asm/arm64/push_registers_masm.S',
                ],
              }],
            ],
          }],
        ],
      },
    },  # v8_heap_base

    ###############################################################################
    # Public targets
    #

    {
      'target_name': 'v8',
      'hard_dependency': 1,
      'toolsets': ['target'],
      'dependencies': [
        'v8_snapshot',
      ],
      'conditions': [
        ['component=="shared_library"', {
          'type': '<(component)',
          'sources': [
            # Note: on non-Windows we still build this file so that gyp
            # has some sources to link into the component.
            '<(V8_ROOT)/src/utils/v8dll-main.cc',
          ],
          'defines': [
            'BUILDING_V8_SHARED',
          ],
          'direct_dependent_settings': {
            'defines': [
              'USING_V8_SHARED',
            ],
          },
          'conditions': [
            ['OS=="mac"', {
              'xcode_settings': {
                'OTHER_LDFLAGS': ['-dynamiclib', '-all_load']
              },
            }],
            ['soname_version!=""', {
              'product_extension': 'so.<(soname_version)',
            }],
          ],
        },
         {
           'type': 'static_library',
         }],
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '<(V8_ROOT)/include',
        ],
      },
    },  # v8
    # missing a bunch of fuzzer targets

    ###############################################################################
    # Protobuf targets, used only when building outside of chromium.
    #

    {
      'target_name': 'postmortem-metadata',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'dependencies': ['run_torque'],
      'variables': {
        'heapobject_files': [
          '<(SHARED_INTERMEDIATE_DIR)/torque-generated/instance-types.h',
          '<(V8_ROOT)/src/objects/allocation-site.h',
          '<(V8_ROOT)/src/objects/allocation-site-inl.h',
          '<(V8_ROOT)/src/objects/cell.h',
          '<(V8_ROOT)/src/objects/cell-inl.h',
          '<(V8_ROOT)/src/objects/dependent-code.h',
          '<(V8_ROOT)/src/objects/dependent-code-inl.h',
          '<(V8_ROOT)/src/objects/bytecode-array.h',
          '<(V8_ROOT)/src/objects/bytecode-array-inl.h',
          '<(V8_ROOT)/src/objects/abstract-code.h',
          '<(V8_ROOT)/src/objects/abstract-code-inl.h',
          '<(V8_ROOT)/src/objects/instruction-stream.h',
          '<(V8_ROOT)/src/objects/instruction-stream-inl.h',
          '<(V8_ROOT)/src/objects/code.h',
          '<(V8_ROOT)/src/objects/code-inl.h',
          '<(V8_ROOT)/src/objects/data-handler.h',
          '<(V8_ROOT)/src/objects/data-handler-inl.h',
          '<(V8_ROOT)/src/objects/deoptimization-data.h',
          '<(V8_ROOT)/src/objects/deoptimization-data-inl.h',
          '<(V8_ROOT)/src/objects/descriptor-array.h',
          '<(V8_ROOT)/src/objects/descriptor-array-inl.h',
          '<(V8_ROOT)/src/objects/feedback-cell.h',
          '<(V8_ROOT)/src/objects/feedback-cell-inl.h',
          '<(V8_ROOT)/src/objects/fixed-array.h',
          '<(V8_ROOT)/src/objects/fixed-array-inl.h',
          '<(V8_ROOT)/src/objects/heap-number.h',
          '<(V8_ROOT)/src/objects/heap-number-inl.h',
          '<(V8_ROOT)/src/objects/heap-object.h',
          '<(V8_ROOT)/src/objects/heap-object-inl.h',
          '<(V8_ROOT)/src/objects/instance-type.h',
          '<(V8_ROOT)/src/objects/instance-type-checker.h',
          '<(V8_ROOT)/src/objects/instance-type-inl.h',
          '<(V8_ROOT)/src/objects/js-array-buffer.h',
          '<(V8_ROOT)/src/objects/js-array-buffer-inl.h',
          '<(V8_ROOT)/src/objects/js-array.h',
          '<(V8_ROOT)/src/objects/js-array-inl.h',
          '<(V8_ROOT)/src/objects/js-function-inl.h',
          '<(V8_ROOT)/src/objects/js-function.cc',
          '<(V8_ROOT)/src/objects/js-function.h',
          '<(V8_ROOT)/src/objects/js-objects.cc',
          '<(V8_ROOT)/src/objects/js-objects.h',
          '<(V8_ROOT)/src/objects/js-objects-inl.h',
          '<(V8_ROOT)/src/objects/js-promise.h',
          '<(V8_ROOT)/src/objects/js-promise-inl.h',
          '<(V8_ROOT)/src/objects/js-raw-json.cc',
          '<(V8_ROOT)/src/objects/js-raw-json.h',
          '<(V8_ROOT)/src/objects/js-raw-json-inl.h',
          '<(V8_ROOT)/src/objects/js-regexp.cc',
          '<(V8_ROOT)/src/objects/js-regexp.h',
          '<(V8_ROOT)/src/objects/js-regexp-inl.h',
          '<(V8_ROOT)/src/objects/js-regexp-string-iterator.h',
          '<(V8_ROOT)/src/objects/js-regexp-string-iterator-inl.h',
          '<(V8_ROOT)/src/objects/map.cc',
          '<(V8_ROOT)/src/objects/map.h',
          '<(V8_ROOT)/src/objects/map-inl.h',
          '<(V8_ROOT)/src/objects/megadom-handler.h',
          '<(V8_ROOT)/src/objects/megadom-handler-inl.h',
          '<(V8_ROOT)/src/objects/name.h',
          '<(V8_ROOT)/src/objects/name-inl.h',
          '<(V8_ROOT)/src/objects/objects.h',
          '<(V8_ROOT)/src/objects/objects-inl.h',
          '<(V8_ROOT)/src/objects/oddball.h',
          '<(V8_ROOT)/src/objects/oddball-inl.h',
          '<(V8_ROOT)/src/objects/primitive-heap-object.h',
          '<(V8_ROOT)/src/objects/primitive-heap-object-inl.h',
          '<(V8_ROOT)/src/objects/scope-info.h',
          '<(V8_ROOT)/src/objects/scope-info-inl.h',
          '<(V8_ROOT)/src/objects/script.h',
          '<(V8_ROOT)/src/objects/script-inl.h',
          '<(V8_ROOT)/src/objects/shared-function-info.cc',
          '<(V8_ROOT)/src/objects/shared-function-info.h',
          '<(V8_ROOT)/src/objects/shared-function-info-inl.h',
          '<(V8_ROOT)/src/objects/string.cc',
          '<(V8_ROOT)/src/objects/string-comparator.cc',
          '<(V8_ROOT)/src/objects/string-comparator.h',
          '<(V8_ROOT)/src/objects/string.h',
          '<(V8_ROOT)/src/objects/string-inl.h',
          '<(V8_ROOT)/src/objects/struct.h',
          '<(V8_ROOT)/src/objects/struct-inl.h',
          '<(V8_ROOT)/src/objects/tagged.h',
        ],
      },
      'actions': [
        {
          'action_name': 'gen-postmortem-metadata',
          'inputs': [
            '<(V8_ROOT)/tools/gen-postmortem-metadata.py',
            '<@(heapobject_files)',
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/debug-support.cc',
          ],
          'action': [
            '<(python)',
            '<(V8_ROOT)/tools/gen-postmortem-metadata.py',
            '<@(_outputs)',
            '<@(heapobject_files)'
          ],
        },
      ],
      'direct_dependent_settings': {
        'sources': ['<(SHARED_INTERMEDIATE_DIR)/debug-support.cc', ],
      },
    },  # postmortem-metadata

    {
      'target_name': 'v8_zlib',
      'type': 'static_library',
      'toolsets': ['host', 'target'],
      'conditions': [
        ['OS=="win"', {
          'conditions': [
            ['"<(target_arch)"=="arm64" and _toolset=="target"', {
              'defines': ['CPU_NO_SIMD']
            }, {
              'defines': ['X86_WINDOWS']
            }]
          ]
        }],
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '<(V8_ROOT)/third_party/zlib',
          '<(V8_ROOT)/third_party/zlib/google',
        ],
      },
      'defines': [ 'ZLIB_IMPLEMENTATION' ],
      'include_dirs': [
        '<(V8_ROOT)/third_party/zlib',
        '<(V8_ROOT)/third_party/zlib/google',
      ],
      'sources': [
        '<(V8_ROOT)/third_party/zlib/adler32.c',
        '<(V8_ROOT)/third_party/zlib/chromeconf.h',
        '<(V8_ROOT)/third_party/zlib/compress.c',
        '<(V8_ROOT)/third_party/zlib/contrib/optimizations/insert_string.h',
        '<(V8_ROOT)/third_party/zlib/contrib/optimizations/insert_string.h',
        '<(V8_ROOT)/third_party/zlib/cpu_features.c',
        '<(V8_ROOT)/third_party/zlib/cpu_features.h',
        '<(V8_ROOT)/third_party/zlib/crc32.c',
        '<(V8_ROOT)/third_party/zlib/crc32.h',
        '<(V8_ROOT)/third_party/zlib/deflate.c',
        '<(V8_ROOT)/third_party/zlib/deflate.h',
        '<(V8_ROOT)/third_party/zlib/gzclose.c',
        '<(V8_ROOT)/third_party/zlib/gzguts.h',
        '<(V8_ROOT)/third_party/zlib/gzlib.c',
        '<(V8_ROOT)/third_party/zlib/gzread.c',
        '<(V8_ROOT)/third_party/zlib/gzwrite.c',
        '<(V8_ROOT)/third_party/zlib/infback.c',
        '<(V8_ROOT)/third_party/zlib/inffast.c',
        '<(V8_ROOT)/third_party/zlib/inffast.h',
        '<(V8_ROOT)/third_party/zlib/inffixed.h',
        '<(V8_ROOT)/third_party/zlib/inflate.c',
        '<(V8_ROOT)/third_party/zlib/inflate.h',
        '<(V8_ROOT)/third_party/zlib/inftrees.c',
        '<(V8_ROOT)/third_party/zlib/inftrees.h',
        '<(V8_ROOT)/third_party/zlib/trees.c',
        '<(V8_ROOT)/third_party/zlib/trees.h',
        '<(V8_ROOT)/third_party/zlib/uncompr.c',
        '<(V8_ROOT)/third_party/zlib/zconf.h',
        '<(V8_ROOT)/third_party/zlib/zlib.h',
        '<(V8_ROOT)/third_party/zlib/zutil.c',
        '<(V8_ROOT)/third_party/zlib/zutil.h',
        '<(V8_ROOT)/third_party/zlib/google/compression_utils_portable.cc',
        '<(V8_ROOT)/third_party/zlib/google/compression_utils_portable.h',
      ],
    },  # v8_zlib
    {
      'target_name': 'fp16',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'variables': {
        'FP16_ROOT': '../../deps/v8/third_party/fp16',
      },
      'direct_dependent_settings': {
        'include_dirs': [
          '<(FP16_ROOT)/src/include',
        ],
      },
    },  # fp16
  ],
}
