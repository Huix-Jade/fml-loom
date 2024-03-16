/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.configuration.providers.mappings.fml_mcp;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.api.mappings.layered.spec.FileSpec;
import net.fabricmc.loom.api.mappings.layered.spec.MCPMappingsSpecBuilder;

import java.util.Optional;

public class MCPMappingsSpecBuilderImpl implements MCPMappingsSpecBuilder {
	/**
	 * The mapping path of regular mapping dependencies.
	 */
	private static final String DEFAULT_MAPPING_PATH = "mappings.tiny";

	private final Optional<FileSpec> fmlFileSpec;
    private final String fallbackSourceNamespace = MappingsNamespace.OFFICIAL.toString();
	private final String fallbackTargetNamespace = MappingsNamespace.NAMED.toString();
    private String mergeNamespace = MappingsNamespace.OFFICIAL.toString();

	private MCPMappingsSpecBuilderImpl(Optional<FileSpec> fileSpec) {
		this.fmlFileSpec = fileSpec;
	}

	public static MCPMappingsSpecBuilderImpl builder(Optional<FileSpec> fileSpec) {
		return new MCPMappingsSpecBuilderImpl(fileSpec);
	}

	public MCPMappingsSpec build() {
        return new MCPMappingsSpec(fmlFileSpec, DEFAULT_MAPPING_PATH, fallbackSourceNamespace, fallbackTargetNamespace, mergeNamespace);
	}
}
